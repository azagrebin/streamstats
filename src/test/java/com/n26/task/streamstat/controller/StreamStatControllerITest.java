package com.n26.task.streamstat.controller;

import com.n26.task.streamstat.StreamStatWebApp;
import com.n26.task.streamstat.model.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StreamStatWebApp.class)
@WebAppConfiguration
public class StreamStatControllerITest {
    private MediaType contentType = new MediaType(
            MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private MockMvc mockMvc;

    private HttpMessageConverter<Object> mappingJackson2HttpMessageConverter;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    void setConverters(HttpMessageConverter<Object>[] converters) {
        this.mappingJackson2HttpMessageConverter = Arrays.stream(converters)
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter)
                .findAny()
                .orElse(null);

        assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void test() throws Exception {
        long ts = System.currentTimeMillis();

        mockMvc.perform(post("/transactions")
                .content(this.json(new Transaction(1.0, ts - 100)))
                .contentType(contentType))
                .andExpect(status().is(201));

        mockMvc.perform(post("/transactions")
                .content(this.json(new Transaction(2.0, ts - 200)))
                .contentType(contentType))
                .andExpect(status().is(201));

        mockMvc.perform(post("/transactions")
                .content(this.json(new Transaction(3.0, ts - 300)))
                .contentType(contentType))
                .andExpect(status().is(201));

        mockMvc.perform(post("/transactions")
                .content(this.json(new Transaction(4.0, 1000)))
                .contentType(contentType))
                .andExpect(status().is(204));

        mockMvc.perform(get("/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.sum", is(6.0)))
                .andExpect(jsonPath("$.count", is(3)))
                .andExpect(jsonPath("$.avg", is(2.0)))
                .andExpect(jsonPath("$.min", is(1.0)))
                .andExpect(jsonPath("$.max", is(3.0)));
    }

    private String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        this.mappingJackson2HttpMessageConverter.write(
                o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}