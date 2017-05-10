package org.sagebionetworks.repo.web.service.dataaccess;

import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.RestrictionInformation;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalRequest;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalResult;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;

public interface DataAccessService {

	ResearchProject createOrUpdate(Long userId, ResearchProject toCreate);

	ResearchProject getUserOwnResearchProjectForUpdate(Long userId, String accessRequirementId);

	RequestInterface createOrUpdate(Long userId, RequestInterface toCreateOrUpdate);

	RequestInterface getRequestForUpdate(Long userId, String requirementId);

	SubmissionStatus submit(Long userId, String requestId, String etag);

	SubmissionStatus cancel(Long userId, String submissionId);

	Submission updateState(Long userId, SubmissionStateChangeRequest request);

	SubmissionPage listSubmissions(Long userId, SubmissionPageRequest request);

	AccessRequirementStatus getAccessRequirementStatus(Long userId, String requirementId);

	RestrictionInformation getRestrictionInformation(Long userId, String entityId);

	OpenSubmissionPage getOpenSubmissions(Long userId, String nextPageToken);

	BatchAccessApprovalResult getAccessApprovalInfo(Long userId, BatchAccessApprovalRequest batchRequest);

}
