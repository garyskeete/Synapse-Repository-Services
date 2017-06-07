package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalManager {
	
	/**
	 *  create access approval
	 */
	public AccessApproval createAccessApproval(UserInfo userInfo, AccessApproval accessApproval) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param approvalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessApproval getAccessApproval(UserInfo userInfo, String approvalId) throws DatastoreException, NotFoundException;

	/**
	 *  get all the access approvals for an entity
	 */
	public List<AccessApproval> getAccessApprovalsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId, Long limit, Long offset) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/*
	 *  delete an access approval
	 */
	public void deleteAccessApproval(UserInfo userInfo, String AccessApprovalId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Delete all access approvals that gives accessorId access to subject(s) that requires access requirement accessRequirementId
	 * 
	 * @param userInfo - the user who making the request
	 * @param accessRequirementId - the target access requirement
	 * @param accessorId - the user whose access is being revoked
	 * @throws UnauthorizedException - if the user is not an admin or an ACT member
	 */
	public void deleteAccessApprovals(UserInfo userInfo, String accessRequirementId, String accessorId) throws UnauthorizedException;

	/**
	 * Delete a batch of AccessApproval
	 * 
	 * @param userInfo
	 * @param toDelete
	 * @return
	 */
	public Count deleteBatch(UserInfo userInfo, IdList toDelete);
}
