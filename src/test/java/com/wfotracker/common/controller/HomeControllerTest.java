package com.wfotracker.common.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.wfotracker.common.security.CustomUserDetails;
import com.wfotracker.domain.entity.Role;
import com.wfotracker.domain.entity.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private HomeController homeController;

    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    private void buildMockMvc() {
        userDetails = new CustomUserDetails(user);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");

        HandlerMethodArgumentResolver argumentResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory) {
                return user.getRoles().isEmpty() ? null : userDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(homeController)
                .setViewResolvers(viewResolver)
                .setCustomArgumentResolvers(argumentResolver)
                .build();
    }

    @Test
    void testHome_Anonymous() throws Exception {
        buildMockMvc();
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void testHome_Admin() throws Exception {
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        user.getRoles().add(adminRole);

        buildMockMvc();
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void testHome_Manager() throws Exception {
        Role managerRole = new Role();
        managerRole.setName("ROLE_MANAGER");
        user.getRoles().add(managerRole);

        buildMockMvc();
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/manager/dashboard"));
    }

    @Test
    void testHome_Employee() throws Exception {
        Role employeeRole = new Role();
        employeeRole.setName("ROLE_EMPLOYEE");
        user.getRoles().add(employeeRole);

        buildMockMvc();
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/employee/dashboard"));
    }

    @Test
    void testAccessDenied() throws Exception {
        buildMockMvc();
        mockMvc.perform(get("/error/403")).andExpect(status().isOk()).andExpect(view().name("error/403"));
    }
}
