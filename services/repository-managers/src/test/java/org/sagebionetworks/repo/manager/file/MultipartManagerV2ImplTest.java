package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.file.MultipartManagerV2Impl.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.upload.multipart.S3MultipartUploadDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class MultipartManagerV2ImplTest {
	
	@Mock
	UserInfo userInfo;
	@Mock
	MultipartUploadDAO mockMultiparUploadDAO;
	@Mock
	ProjectSettingsManager mockProjectSettingsManager;
	@Mock
	S3MultipartUploadDAO mockS3multipartUploadDAO;

	MultipartUploadRequest request;	
	String requestJson;
	String requestHash;
	MultipartManagerV2Impl manager;
	Boolean forceRestart;
	
	String uploadToken;
	CompositeMultipartUploadStatus composite;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		when(userInfo.getId()).thenReturn(456L);
		
		request = new MultipartUploadRequest();
		request.setFileName("foo.txt");
		request.setFileSizeBytes((long) (1024*1024*100+15));
		request.setContentMD5Hex("someMD5Hex");
		request.setPartSizeBytes((long) (1024*1024*5));
		request.setStorageLocationId(789L);
		
		requestJson = createRequestJSON(request);
		requestHash = calculateMD5AsHex(request);
		
		MultipartUploadStatus status = new MultipartUploadStatus();
		status.setUploadId("123456");
		composite = new CompositeMultipartUploadStatus();
		composite.setMultipartUploadStatus(status);
		
		uploadToken = "someUploadToken";
		
		when(mockMultiparUploadDAO.createUploadStatus(any(CreateMultipartRequest.class))).thenReturn(composite);
		when(mockS3multipartUploadDAO.initiateMultipartUpload(anyString(), anyString(), any(MultipartUploadRequest.class))).thenReturn(uploadToken);

				
		forceRestart = null;
		manager = new MultipartManagerV2Impl();
		ReflectionTestUtils.setField(manager, "multiparUploadDAO", mockMultiparUploadDAO);
		ReflectionTestUtils.setField(manager, "projectSettingsManager", mockProjectSettingsManager);
		ReflectionTestUtils.setField(manager, "s3multipartUploadDAO", mockS3multipartUploadDAO);
		
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testStartOrResumeMultipartUploadAnonymous(){
		// set the user to anonymous
		when(userInfo.getId()).thenReturn(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		//call under test
		manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
	}
	

	@Test
	public void testCalculateMD5AsHex(){
		// This md5 was generated from the json string of the request.
		String expected = "8356accbaa8bfc6ddc6c612224c6c9b3";
		//call under test
		String md5Hex = MultipartManagerV2Impl.calculateMD5AsHex(request);
		assertEquals(expected, md5Hex);
	}
	
	@Test
	public void testCalculateNumberOfPartsSmall(){
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(1, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfPartsNoRemainder(){
		long fileSize = MIN_PART_SIZE_BYTES*2;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(2, numberOfParts);
	}
	
	@Test
	public void testCalculateNumberOfPartsWithRemainder(){
		long fileSize = MIN_PART_SIZE_BYTES*2+1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(3, numberOfParts);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNumberOfLessThanOne(){
		long fileSize = 0;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNumberOfPartTooSmall(){
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES-1;
		//call under test
		MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
	}
	
	@Test
	public void testCalculateNumberOfPartAtMax(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(MAX_NUMBER_OF_PARTS, numberOfParts);
	}
	
	@Test
	public void testCalculateNumberOfPartOverMax(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS+1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		try {
			MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("10001"));
		}
	}
	
	@Test
	public void testStartOrResumeMultipartUploadNotStarted(){
		// setup the case where the status does not exist
		when(mockMultiparUploadDAO.getUploadStatus(anyLong(), anyString())).thenReturn(null);
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		
		// since the upload does not exist it should get created.
		ArgumentCaptor<CreateMultipartRequest> requestCapture = ArgumentCaptor.forClass(CreateMultipartRequest.class);
		verify(mockMultiparUploadDAO).createUploadStatus(requestCapture.capture());
		assertEquals(uploadToken, requestCapture.getValue().getUploadToken());
		assertEquals(StackConfiguration.getS3Bucket(), requestCapture.getValue().getBucket());
		assertNotNull(requestCapture.getValue().getKey());
		assertEquals(requestJson, requestCapture.getValue().getRequestBody());
		assertEquals(userInfo.getId(), requestCapture.getValue().getUserId());
		assertEquals(requestHash, requestCapture.getValue().getHash());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadAlreadyStarted(){
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(anyLong(), anyString())).thenReturn(composite);
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		verify(mockMultiparUploadDAO, never()).createUploadStatus(any(CreateMultipartRequest.class));
	}
	
	@Test
	public void testStartOrResumeMultipartUploadForceRestartFalse(){
		forceRestart = Boolean.FALSE;
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadForceRestartTrue(){
		forceRestart = Boolean.TRUE;
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		verify(mockMultiparUploadDAO).deleteUploadStatus(anyLong(), anyString());
	}
}