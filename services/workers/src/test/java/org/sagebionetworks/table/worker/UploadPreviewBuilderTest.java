package org.sagebionetworks.table.worker;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewRequest;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;

import au.com.bytecode.opencsv.CSVReader;

public class UploadPreviewBuilderTest {
	
	@Mock
	ProgressCallback<Integer> mockProgressCallback;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testBuildResultsWithHeader() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "string", "int", "boolean", "double" });
		input.add(new String[] { "CCC", null, "false", "1.1" });
		// include empties
		input.add(new String[] { "", "", "", "", });
		// included nulls
		input.add(new String[] { null, null, null, null, });
		input.add(new String[] { "FFF", "4", "true", "2.2", });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		List<ColumnModel> expectedSchema = new ArrayList<ColumnModel>();
		// 0
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("string");
		cm.setMaximumSize(3L);
		expectedSchema.add(cm);
		// 1
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("int");
		expectedSchema.add(cm);
		// 2
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setName("boolean");
		expectedSchema.add(cm);
		// 3
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.DOUBLE);
		cm.setName("double");
		expectedSchema.add(cm);
		
		List<Row> expectedRows = new ArrayList<Row>();
		// 0
		Row row = new Row();
		row.setValues(Arrays.asList(input.get(1)));
		expectedRows.add(row);
		// 1
		row = new Row();
		row.setValues(Arrays.asList(input.get(2)));
		expectedRows.add(row);
		// 2
		row = new Row();
		row.setValues(Arrays.asList(input.get(3)));
		expectedRows.add(row);
		// 3
		row = new Row();
		row.setValues(Arrays.asList(input.get(4)));
		expectedRows.add(row);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(4), result.getRowsScanned());
		assertEquals(expectedSchema, result.getSuggestedColumns());
		assertEquals(expectedRows, result.getSampleRows());
		verify(mockProgressCallback, times(4)).progressMade(anyInt());;
	}
	
	@Test
	public void testBuildResultsNoHeader() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "CCC", "3" });
		input.add(new String[] { "FFF", "4" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		List<ColumnModel> expectedSchema = new ArrayList<ColumnModel>();
		// 0
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("col1");
		cm.setMaximumSize(3L);
		expectedSchema.add(cm);
		// 1
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("col2");
		expectedSchema.add(cm);
		
		List<Row> expectedRows = new ArrayList<Row>();
		// 0
		Row row = new Row();
		row.setValues(Arrays.asList(input.get(0)));
		expectedRows.add(row);
		// 1
		row = new Row();
		row.setValues(Arrays.asList(input.get(1)));
		expectedRows.add(row);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(2), result.getRowsScanned());
		assertEquals(expectedSchema, result.getSuggestedColumns());
		assertEquals(expectedRows, result.getSampleRows());
		verify(mockProgressCallback, times(2)).progressMade(anyInt());;
	}

	
	@Test
	public void testBuildResultsWithRowIdAndVersion() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// This CSV has ROW_ID and ROW_VERSION
		input.add(new String[] {ROW_ID, ROW_VERSION, "boolean", "double" });
		input.add(new String[] { "1", "0", "false", "1.1" });
		input.add(new String[] { "2", "0", "true", "2.2", });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		// RowId and version should not be included as suggested columns.
		List<ColumnModel> expectedSchema = new ArrayList<ColumnModel>();
		// 0
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.BOOLEAN);
		cm.setName("boolean");
		expectedSchema.add(cm);
		// 1
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.DOUBLE);
		cm.setName("double");
		expectedSchema.add(cm);
		
		List<Row> expectedRows = new ArrayList<Row>();
		// 0
		Row row = new Row();
		row.setValues(Arrays.asList("false", "1.1"));
		row.setRowId(1L);
		row.setVersionNumber(0L);
		expectedRows.add(row);
		// 1
		row = new Row();
		row.setValues(Arrays.asList("true", "2.2"));
		row.setRowId(2L);
		row.setVersionNumber(0L);;
		expectedRows.add(row);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(2), result.getRowsScanned());
		assertEquals(expectedRows, result.getSampleRows());
		assertEquals(expectedSchema, result.getSuggestedColumns());
	}
	
	@Test
	public void testPLFM_3887_MissingRowID() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// This CSV has ROW_ID and ROW_VERSION
		input.add(new String[] {ROW_ID, ROW_VERSION, "s", "i" });
		input.add(new String[] { "1", "0", "a2", "7" });
		input.add(new String[] { "", "", "a1", "6", });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSampleRows());
		assertEquals(2, result.getSampleRows().size());
		Row row = result.getSampleRows().get(1);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
	}
	
	@Test
	public void testPLFM_3887_NullRowId() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// This CSV has ROW_ID and ROW_VERSION
		input.add(new String[] {ROW_ID, ROW_VERSION, "s", "i" });
		input.add(new String[] { "1", "0", "a2", "7" });
		input.add(new String[] { null, null, "a1", "6", });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSampleRows());
		assertEquals(2, result.getSampleRows().size());
		Row row = result.getSampleRows().get(1);
		assertNull(row.getRowId());
		assertNull(row.getVersionNumber());
	}
	
	@Test
	public void testPLFM_3887_DeleteRow() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// This CSV has ROW_ID and ROW_VERSION
		input.add(new String[] {ROW_ID, ROW_VERSION, "s", "i" });
		input.add(new String[] { "1", "0"});
		input.add(new String[] { "2", "1", "a1", "6", });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result);
		assertNotNull(result.getSampleRows());
		assertEquals(2, result.getSampleRows().size());
		// one
		Row row = result.getSampleRows().get(0);
		assertEquals(new Long(1),row.getRowId());
		assertEquals(new Long(0), row.getVersionNumber());
		assertNotNull(row.getValues());
		assertNull(row.getValues().get(0));
		assertNull(row.getValues().get(1));
		// two
		row = result.getSampleRows().get(1);
		assertEquals(new Long(2),row.getRowId());
		assertEquals(new Long(1), row.getVersionNumber());
		assertNotNull(row.getValues());
		assertEquals("a1",row.getValues().get(0));
		assertEquals("6",row.getValues().get(1));
	}
	
	@Test
	public void testPLFM_3887_SparseRows() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// This CSV has ROW_ID and ROW_VERSION
		input.add(new String[] {"a","b","c"});
		input.add(new String[] { "1", "2", "3"});
		input.add(new String[] { "4", "5", null});
		input.add(new String[] { "6", "7"});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
	}
	
	@Test
	public void testBuildResultsFullScan() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "CCC", "3" });
		input.add(new String[] { "FFF", "4" });
		input.add(new String[] { "GGG", "5" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		// A full scan should include all rows.
		request.setDoFullFileScan(true);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()+1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(3), result.getRowsScanned());
	}
	
	@Test
	public void testBuildResultsPartialScan() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "CCC", "3" });
		input.add(new String[] { "FFF", "4" });
		input.add(new String[] { "GGG", "5" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		// We do not want all row scanned.
		request.setDoFullFileScan(false);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(2), result.getRowsScanned());
	}
	
	/**
	 * Test for the case where there is no data in one of the columns.
	 * @throws IOException
	 */
	@Test
	public void testPLFM_3032() throws IOException{
		List<String[]> input = new ArrayList<String[]>(4);
		input.add(new String[] { "A", "B","C"});
		input.add(new String[] { "CCC", "3", null });
		input.add(new String[] { "FFF", "4", null });
		input.add(new String[] { "GGG", "5", null });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		request.setDoFullFileScan(true);
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(3), result.getRowsScanned());
		assertNotNull(result.getSuggestedColumns());
		assertEquals(3, result.getSuggestedColumns().size());
		ColumnModel third = result.getSuggestedColumns().get(2);
		assertEquals("C", third.getName());
		assertEquals(ColumnType.STRING, third.getColumnType());
		assertEquals(new Long(50), third.getMaximumSize());
	}
	

	/**
	 * PLFM-3191 occurs when the first line is not a header 
	 * and at least one column contains all empty strings.
	 * @throws IOException
	 */
	@Test
	public void testPLFM_3191() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "EEE", "", "foo" });
		input.add(new String[] { "FFF", "", "bar" });
		input.add(new String[] { "GGG", "", "foo" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		request.setDoFullFileScan(true);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(3), result.getRowsScanned());
		List<String> names = getColumnNames(result.getSuggestedColumns());
		// Default headers should be assigned.
		assertEquals(Arrays.asList("col1","col2","col3"), names);
	}
	
	/**
	 * PLFM-3190 is cause by csv with a single row.
	 * @throws IOException
	 */
	@Test
	public void testPLFM_3190() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "EEE", "123", "foo" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		request.setDoFullFileScan(true);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(0), result.getRowsScanned());
		
		List<String> names = getColumnNames(result.getSuggestedColumns());
		// The names should match the the one row
		assertEquals(Arrays.asList(input.get(0)), names);
	}
	
	@Test
	public void testOneRowNoHeader() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] { "EEE", "123", "foo" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(false);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		request.setDoFullFileScan(true);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(1), result.getRowsScanned());
		List<String> names = getColumnNames(result.getSuggestedColumns());
		// Default headers should be assigned.
		assertEquals(Arrays.asList("col1","col2","col3"), names);
	}

	@Test
	public void testBadHeaders() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// Note DATE, ALL, AVG are SQL reserved words and cannot be used as column headers.
		input.add(new String[] { ROW_ID, ROW_VERSION, "Date", "all", "avg", "\"quote\"", "~!@#$%^&* ()_+{}:<>?/.,;'", "okay", "all", "col" });
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		request.setDoFullFileScan(true);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(0), result.getRowsScanned());
		
		List<String> names = getColumnNames(result.getSuggestedColumns());
		//The first two should not be changed.
		assertEquals(Arrays.asList("_Date", "_all", "_avg", "quote", "col", "okay", "_all1", "col1"), names);
	}
	
	@Test
	public void testNonUniqueHeaders() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// Note DATE, ALL, AVG are SQL reserved words and cannot be used as column headers.
		input.add(new String[] { "a","b","c","a","b","c","a","b","c"});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		request.setDoFullFileScan(true);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// Set the max beyond the number of rows we have.
		builder.setMaxRowsInpartialScan(input.size()-1);
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertEquals(new Long(0), result.getRowsScanned());
		
		List<String> names = getColumnNames(result.getSuggestedColumns());
		//The first two should not be changed.
		assertEquals(Arrays.asList("a","b","c","a1","b1","c1","a2","b2","c2"), names);
	}
	
	@Test
	public void testProcessHeaderKeyWord(){
		// prefix key words with underscore
		assertEquals("_AS", UploadPreviewBuilder.processHeader("AS"));
	}
	
	@Test
	public void testProcessHeaderWhiteSpace(){
		// prefix with underscore
		assertEquals("foo_bar_bar", UploadPreviewBuilder.processHeader("\nfoo bar\tbar\n"));
	}
	
	@Test
	public void testProcessHeaderWord(){
		// ROW_ID is a reserved column name and can be used
		assertEquals(ROW_ID, UploadPreviewBuilder.processHeader(ROW_ID));
	}
	
	@Test
	public void testProcessHeaderAllBad(){
		// If there is nothing usable in the name then default to 'col'
		assertEquals("col", UploadPreviewBuilder.processHeader("~!@#$%^&*()+\"<>?,./;'[]{}"));
	}
	
	@Test
	public void testProcessHeaderSomeGood(){
		// Keep any good elements that we can
		assertEquals("abcd_e_g", UploadPreviewBuilder.processHeader("~!@a#$%^&*b()+\"<c>?,./;d'[] {}e_g"));
	}
	
	@Test
	public void testProcessHeaderNull(){
		// default to col
		assertEquals("col", UploadPreviewBuilder.processHeader(null));
	}
	
	@Test
	public void testProcessHeaderEmpty(){
		// default to col
		assertEquals("col", UploadPreviewBuilder.processHeader(" \n\t"));
	}
	
	@Test
	public void testProcessHeaderUnderscoreLeft(){
		// default when there would be nothing but underscores.
		assertEquals("col", UploadPreviewBuilder.processHeader("#_&_#"));
	}
	
	@Test
	public void testPLFM_4165() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		// This CSV has ROW_ID and ROW_VERSION
		input.add(new String[] {"NCBI_Build","Chromosome"});
		input.add(new String[] { "GRCh37", "13"});
		input.add(new String[] { "GRCh37", "X"});
		input.add(new String[] { "GRCh37", "15"});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSuggestedColumns());
		assertEquals(2, result.getSuggestedColumns().size());
		// one
		ColumnModel cm = result.getSuggestedColumns().get(0);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals("NCBI_Build", cm.getName());
		assertEquals(new Long(6), cm.getMaximumSize());
		// two
		cm = result.getSuggestedColumns().get(1);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals("Chromosome", cm.getName());
		assertEquals(new Long(2), cm.getMaximumSize());
	}
	
	@Test
	public void testBooleanToString() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] {"mixed"});
		input.add(new String[] { "true"});
		input.add(new String[] { "X",});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSuggestedColumns());
		assertEquals(1, result.getSuggestedColumns().size());
		// one
		ColumnModel cm = result.getSuggestedColumns().get(0);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals("mixed", cm.getName());
		assertEquals(new Long(4), cm.getMaximumSize());
	}
	
	@Test
	public void testStringToBoolean() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] {"mixed"});
		input.add(new String[] { "X"});
		input.add(new String[] { "true",});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSuggestedColumns());
		assertEquals(1, result.getSuggestedColumns().size());
		// one
		ColumnModel cm = result.getSuggestedColumns().get(0);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals("mixed", cm.getName());
		assertEquals(new Long(4), cm.getMaximumSize());
	}
	
	
	@Test
	public void testDoubleToString() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] {"mixed"});
		input.add(new String[] { "123.456"});
		input.add(new String[] { "X",});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSuggestedColumns());
		assertEquals(1, result.getSuggestedColumns().size());
		// one
		ColumnModel cm = result.getSuggestedColumns().get(0);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals("mixed", cm.getName());
		assertEquals(new Long(7), cm.getMaximumSize());
	}
	
	@Test
	public void testIntegerToDoubleToString() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] {"mixed"});
		input.add(new String[] { "123456"});
		input.add(new String[] { "2.1",});
		input.add(new String[] { "X",});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSuggestedColumns());
		assertEquals(1, result.getSuggestedColumns().size());
		// one
		ColumnModel cm = result.getSuggestedColumns().get(0);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals("mixed", cm.getName());
		assertEquals(new Long(6), cm.getMaximumSize());
	}
	
	@Test
	public void testAllToString() throws IOException{
		List<String[]> input = new ArrayList<String[]>(3);
		input.add(new String[] {"mixed"});
		input.add(new String[] { "true"});
		input.add(new String[] { "1",});
		input.add(new String[] { "2.1",});
		input.add(new String[] { "X",});
		String eachTypeCSV = TableModelTestUtils.createCSVString(input);
		CsvTableDescriptor descriptor = new CsvTableDescriptor();
		descriptor.setIsFirstLineHeader(true);
		UploadToTablePreviewRequest request = new UploadToTablePreviewRequest();
		request.setCsvTableDescriptor(descriptor);
		
		StringReader sReader = new StringReader(eachTypeCSV);
		CSVReader reader = new CSVReader(sReader);
		UploadPreviewBuilder builder = new UploadPreviewBuilder(reader, mockProgressCallback, request);
		// call under test
		UploadToTablePreviewResult result = builder.buildResult();
		assertNotNull(result);
		assertNotNull(result.getSuggestedColumns());
		assertEquals(1, result.getSuggestedColumns().size());
		// one
		ColumnModel cm = result.getSuggestedColumns().get(0);
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals("mixed", cm.getName());
		assertEquals(new Long(4), cm.getMaximumSize());
	}
	
	@Test
	public void testParseAsLong(){
		assertEquals(null, UploadPreviewBuilder.parseAsLong(null));
		assertEquals(null, UploadPreviewBuilder.parseAsLong(""));
		assertEquals(new Long(123), UploadPreviewBuilder.parseAsLong("123"));
	}
	
	public static List<String> getColumnNames(List<ColumnModel> models){
		if(models == null) throw new IllegalArgumentException("ColumnModels cannot be null");
		List<String> names = new LinkedList<String>();
		for(ColumnModel model: models){
			names.add(model.getName());
		}
		return names;
	}
}
