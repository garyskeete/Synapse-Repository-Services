package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.persistence.table.DBOTableRowChange;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.TableChangeType;
import org.sagebionetworks.repo.model.table.TableRowChange;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;

public class TableRowChangeUtils {
	/**
	 * Convert from the DBO to the DTO
	 * 
	 * @param dbo
	 * @return
	 */
	public static TableRowChange ceateDTOFromDBO(DBOTableRowChange dbo) {
		if (dbo == null)
			throw new IllegalArgumentException("dbo cannot be null");
		TableRowChange dto = new TableRowChange();
		dto.setTableId(KeyFactory.keyToString(dbo.getTableId()));
		dto.setRowVersion(dbo.getRowVersion());
		dto.setEtag(dbo.getEtag());
		if(dbo.getColumnIds() != null){
			dto.setIds(TableModelUtils.readColumnModelIdsFromDelimitedString(dbo.getColumnIds()));
		}
		dto.setCreatedBy(Long.toString(dbo.getCreatedBy()));
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		dto.setBucket(dbo.getBucket());
		dto.setKey(dbo.getKey());
		dto.setKeyNew(dbo.getKeyNew());
		dto.setRowCount(dbo.getRowCount());
		dto.setChangeType(TableChangeType.valueOf(dbo.getChangeType()));
		return dto;
	}

	/**
	 * Create a DBO from the DTO
	 * 
	 * @param dto
	 * @return
	 */
	public static DBOTableRowChange createDBOFromDTO(TableRowChange dto) {
		if (dto == null)
			throw new IllegalArgumentException("dto cannot be null");
		DBOTableRowChange dbo = new DBOTableRowChange();
		dbo.setTableId(KeyFactory.stringToKey(dto.getTableId()));
		dbo.setRowVersion(dto.getRowVersion());
		dbo.setEtag(dto.getEtag());
		if(dto.getIds() != null){
			dbo.setColumnIds(TableModelUtils.createDelimitedColumnModelIdString(dto.getIds()));
		}
		dbo.setCreatedBy(Long.parseLong(dto.getCreatedBy()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		dbo.setBucket(dto.getBucket());
		dbo.setKey(dto.getKey());
		dbo.setKeyNew(dto.getKeyNew());
		dbo.setRowCount(dto.getRowCount());
		dbo.setChangeType(dto.getChangeType().name());
		return dbo;
	}

	/**
	 * Convert a list of DTOs from a list of DBOs
	 * 
	 * @param dbos
	 * @return
	 */
	public static List<TableRowChange> ceateDTOFromDBO(List<DBOTableRowChange> dbos) {
		if (dbos == null)
			throw new IllegalArgumentException("DBOs cannot be null");
		List<TableRowChange> dtos = new LinkedList<TableRowChange>();
		for (DBOTableRowChange dbo : dbos) {
			TableRowChange dto = ceateDTOFromDBO(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

}
