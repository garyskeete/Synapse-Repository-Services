package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This DAO provides status information about a table.
 * 
 * @author John
 * 
 */
public interface TableStatusDAO {


	/**
	 * When the truth data of a table changes either due to row changes or
	 * column changes the table status must be rest to PROCESSING. This is the
	 * only method that will change the reset-token.  The resulting reset-token
	 * will be needed to change the status to available or failed.
	 * 
	 * @param tableId
	 * @return The new reset-token for this table status.
	 */
	public String resetTableStatusToProcessing(String tableId);

	/**
	 * Attempt to set the table status to AVIALABLE. The state will be changed
	 * will be applied as long as the passed resetToken matches the current
	 * restToken indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 * @throws NotFoundException 
	 */
	public void attemptToSetTableStatusToAvailable(String tableId,
			String resetToken) throws ConflictingUpdateException, NotFoundException;

	/**
	 * Attempt to set the table status to FAILED. The state will be changed will
	 * be applied as long as the passed resetToken matches the current restToken
	 * indicating all changes have been accounted for.
	 * 
	 * @param tableId
	 * @param resetToken
	 * @return
	 * @throws ConflictingUpdateException
	 *             Thrown when the passed restToken does not match the current
	 *             resetToken. This indicates that the table was updated before
	 *             processing finished so we cannot change the status to
	 *             available until the new changes are accounted for.
	 */
	public void attemptToSetTableStatusToFailed(String tableId,
			String resetToken, String errorMessage, Throwable errorDetails)
			throws ConflictingUpdateException;

	/**
	 * Get the current status of a table.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 *             Thrown when there is no status information for the given
	 *             table.
	 */
	public TableStatus getTableStatus(String tableId) throws NotFoundException;
	
	/**
	 * Remove all table state.  This should not be called during normal opperations.
	 * 
	 */
	public void clearAllTableState();
}
