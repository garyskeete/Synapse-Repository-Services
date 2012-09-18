package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class PermissionsManagerImpl implements PermissionsManager {
	
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private NodeInheritanceManager nodeInheritanceManager;
	
	@Autowired
	NodeDAO nodeDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserManager userManager;

	@Transactional(readOnly = true)
	@Override
	public AccessControlList getACL(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException, ACLInheritanceException {
		// get the id that this node inherits its permissions from
		String benefactor = nodeInheritanceManager.getBenefactor(nodeId);
		// This is a fix for PLFM-398.
		if(!benefactor.equals(nodeId)){
			// Look up the type of the benefactor
			EntityHeader header = nodeDao.getEntityHeader(benefactor);
			EntityType type = EntityType.getEntityType(header.getType());
			throw new ACLInheritanceException("Cannot access the ACL of a node that inherits it permissions. This node inherits its permissions from: "+benefactor, benefactor);
		}
		AccessControlList acl = aclDAO.getForResource(nodeId);
		//populateDisplayNames(acl);
		return acl;
	}
	
	public void validateContent(AccessControlList acl) throws InvalidModelException {
		if (acl.getId()==null) throw new InvalidModelException("Resource ID is null");
		if(acl.getResourceAccess() == null){
			acl.setResourceAccess(new HashSet<ResourceAccess>());
		}
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra==null) throw new InvalidModelException("ACL row is null.");
			if (ra.getPrincipalId()==null) throw new InvalidModelException("Group ID is null");
			if (ra.getAccessType().isEmpty()) throw new InvalidModelException("No access types specified.");
		}
		
		// If createdBy is not set then set it with the current time.
		if(acl.getCreationDate() == null){
			acl.setCreationDate(new Date(System.currentTimeMillis()));
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList updateACL(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Cannot update ACL for a resource which inherits its permissions.");
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, rId, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		validateContent(acl);
		// Before we can update the ACL we must grab the lock on the node.
		String newETag = nodeDao.lockNodeAndIncrementEtag(acl.getId(), acl.getEtag());
		aclDAO.update(acl);
		acl = aclDAO.get(acl.getId());
		acl.setEtag(newETag);
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList overrideInheritance(AccessControlList acl, UserInfo userInfo) throws NotFoundException, DatastoreException, InvalidModelException, UnauthorizedException, ConflictingUpdateException {
		String rId = acl.getId();
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (benefactor.equals(rId)) throw new UnauthorizedException("Resource already has an ACL.");
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, benefactor, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		// validate content
		validateContent(acl);
		Node node = nodeDao.getNode(rId);
		// Before we can update the ACL we must grab the lock on the node.
		String newEtag = nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		// set permissions 'benefactor' for resource and all resource's descendants to resource
		nodeInheritanceManager.setNodeToInheritFromItself(rId);
		// persist acl and return
		String id = aclDAO.create(acl);
		acl = aclDAO.get(acl.getId());
		acl.setEtag(newEtag);
		return acl;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList restoreInheritance(String rId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, rId, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		String benefactor = nodeInheritanceManager.getBenefactor(rId);
		if (!benefactor.equals(rId)) throw new UnauthorizedException("Resource already inherits its permissions.");	

		// if parent is root, than can't inherit, must have own ACL
		if (nodeDao.isNodesParentRoot(rId)) throw new UnauthorizedException("Cannot restore inheritance for resource which has no parent.");

		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDao.getNode(rId);
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
		nodeInheritanceManager.setNodeToInheritFromNearestParent(rId);
		
		// delete access control list
		AccessControlList acl = aclDAO.getForResource(rId);
		aclDAO.delete(acl.getId());
		
		// now find the newly governing ACL
		benefactor = nodeInheritanceManager.getBenefactor(rId);
		
		return aclDAO.getForResource(benefactor);
	}	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AccessControlList applyInheritanceToChildren(String parentId, UserInfo userInfo) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// check permissions of user to change permissions for the resource
		if (!authorizationManager.canAccess(userInfo, parentId, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
			throw new UnauthorizedException("Not authorized.");
		}
		
		// Before we can update the ACL we must grab the lock on the node.
		Node node = nodeDao.getNode(parentId);
		nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());

		applyInheritanceToChildrenHelper(parentId, userInfo);

		// return governing parent ACL
		return aclDAO.getForResource(nodeInheritanceManager.getBenefactor(parentId));
	}
	
	private void applyInheritanceToChildrenHelper(String parentId, UserInfo userInfo) throws NotFoundException, DatastoreException, ConflictingUpdateException {
		// Get all of the child nodes, sorted by id (to prevent deadlock)
		List<String> children = nodeDao.getChildrenIdsAsList(parentId);
		
		// Update each node
		for(String idToChange: children) {
			String benefactorId = nodeInheritanceManager.getBenefactor(parentId);
			
			// must be authorized to modify permissions
			if (authorizationManager.canAccess(userInfo, idToChange, ACCESS_TYPE.CHANGE_PERMISSIONS)) {
				// delete child ACL, if present
				if (isOwnBenefactor(idToChange)) {
					// Before we can update the ACL we must grab the lock on the node.
					Node node = nodeDao.getNode(idToChange);
					nodeDao.lockNodeAndIncrementEtag(node.getId(), node.getETag());
					
					// delete ACL
					AccessControlList acl = aclDAO.getForResource(idToChange);
					aclDAO.delete(acl.getId());
				}								
				// set benefactor ACL
				nodeInheritanceManager.addBeneficiary(idToChange, benefactorId);
				
				// recursively apply to children
				applyInheritanceToChildrenHelper(idToChange, userInfo);
			}
		}
	}
	
	@Override
	public Collection<UserGroup> getGroups(UserInfo userInfo) throws DatastoreException {
		return getGroups();
	}

	@Override
	public Collection<UserGroup> getGroups() throws DatastoreException {
		List<String> groupsToOmit = new ArrayList<String>();
		groupsToOmit.add(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME);
		return userGroupDAO.getAllExcept(false, groupsToOmit);
	}

	@Override
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException {
		List<String> groupsToOmit = new ArrayList<String>();
		groupsToOmit.add(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME);
		return userGroupDAO.getInRangeExcept(startIncl, endExcl, false, groupsToOmit);
	}

	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	@Override
	public boolean hasAccess(String resourceId, ACCESS_TYPE accessType, UserInfo userInfo) throws NotFoundException, DatastoreException  {
		return authorizationManager.canAccess(userInfo, resourceId, accessType);
	}

	/**
	 * Get the permission benefactor of an entity.
	 * @throws DatastoreException 
	 */
	@Override
	public String getPermissionBenefactor(String nodeId, UserInfo userInfo) throws NotFoundException, DatastoreException {
		return nodeInheritanceManager.getBenefactor(nodeId);
	}

	@Override
	public UserEntityPermissions getUserPermissionsForEntity(UserInfo userInfo,	String entityId) throws NotFoundException, DatastoreException {
		// pass it along the permission object
		return authorizationManager.getUserPermissionsForEntity(userInfo, entityId);
	}

	/**
	 * Is a node its own benefactor? In other words, does a node have a locally-
	 * defined ACL?
	 */
	private boolean isOwnBenefactor(String rId) {
		try {
			return nodeInheritanceManager.getBenefactor(rId).equals(rId);
		} catch (Exception e) {
			return false;
		}
	}
	
}