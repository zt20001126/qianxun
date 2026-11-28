package com.qianxun.homestay.modules.mall;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.qianxun.homestay.common.exception.BusinessException;
import com.qianxun.homestay.common.security.service.JwtService;
import com.qianxun.homestay.modules.storage.service.CosStorageService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 使用真实权限链和模拟 COS 验证商城和客房图片上传接口。 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {"app.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@AutoConfigureMockMvc
class MallImageCosApiIntegrationTest {
    private static final String MALL_PATH = "/api/admin/mall/products/images";
    private static final String ROOM_PATH = "/api/admin/rooms/images";
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("mall_image_cos_test").withUsername("qianxun").withPassword("qianxun-test-password");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwt;
    @MockitoBean private CosStorageService cos;

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "a.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
    }

    @ParameterizedTest
    @ValueSource(strings = {MALL_PATH, ROOM_PATH})
    void adminCanUploadAndGetsCosUrl(String path) throws Exception {
        when(cos.publicUrl(anyString())).thenReturn("https://images.example.com/image.png");
        mvc.perform(multipart(path).file(image()).header("Authorization", "Bearer " + jwt.issueAdminToken(1L)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.url").value("https://images.example.com/image.png"));
    }

    @ParameterizedTest
    @ValueSource(strings = {MALL_PATH, ROOM_PATH})
    void rejectsAnonymousAndMemberBeforeStorage(String path) throws Exception {
        mvc.perform(multipart(path).file(image())).andExpect(status().isUnauthorized());
        mvc.perform(multipart(path).file(image()).header("Authorization", "Bearer " + jwt.issueMemberToken(1L)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(cos);
    }

    @ParameterizedTest
    @ValueSource(strings = {MALL_PATH, ROOM_PATH})
    void rejectsInvalidAndEmptyFiles(String path) throws Exception {
        String token = "Bearer " + jwt.issueAdminToken(1L);
        mvc.perform(multipart(path).file(new MockMultipartFile("file", "x.svg", "image/svg+xml", "<svg/>".getBytes()))
                        .header("Authorization", token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart(path).file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
                        .header("Authorization", token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(cos);
    }

    @ParameterizedTest
    @ValueSource(strings = {MALL_PATH, ROOM_PATH})
    void rejectsMissingFilePartAsBadRequest(String path) throws Exception {
        mvc.perform(multipart(path).header("Authorization", "Bearer " + jwt.issueAdminToken(1L)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求参数不正确"));
        verifyNoInteractions(cos);
    }

    @ParameterizedTest
    @ValueSource(strings = {MALL_PATH, ROOM_PATH})
    void cosErrorProducesSanitizedFailureResponse(String path) throws Exception {
        when(cos.publicUrl(anyString())).thenReturn("https://images.example.com/image.png");
        doThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "腾讯云 COS 上传失败"))
                .when(cos).upload(anyString(), any(), anyLong(), anyString());
        mvc.perform(multipart(path).file(image()).header("Authorization", "Bearer " + jwt.issueAdminToken(1L)))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value(502))
                .andExpect(jsonPath("$.message").value("腾讯云 COS 上传失败"));
    }

    @ParameterizedTest
    @CsvSource({MALL_PATH + ",mall/products/", ROOM_PATH + ",rooms/"})
    void repeatedUploadCreatesTwoDistinctObjects(String path, String directory) throws Exception {
        when(cos.publicUrl(anyString())).thenAnswer(call -> "https://images.example.com/" + call.getArgument(0));
        String token = "Bearer " + jwt.issueAdminToken(1L);
        mvc.perform(multipart(path).file(image()).header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(multipart(path).file(image()).header("Authorization", token)).andExpect(status().isOk());
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(cos, times(2)).upload(keys.capture(), any(), anyLong(), anyString());
        assertThat(keys.getAllValues()).doesNotHaveDuplicates()
                .allSatisfy(key -> assertThat(key).startsWith(directory).endsWith(".png"));
    }
}
