package com.aeliya.orbit.modules.dashboard;

import com.aeliya.orbit.modules.employee.service.ProfileFileStore;
import com.aeliya.orbit.modules.workday.WorkdayController;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BirthdayPortraitRouteTest {
    @TempDir java.nio.file.Path temp;

    @Test void carouselPortraitUrlReturnsStoredImage() throws Exception {
        var jdbc = mock(JdbcTemplate.class);
        var workday = mock(WorkdayController.class);
        var files = mock(ProfileFileStore.class);
        var today = LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
        byte[] portrait = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0};
        var path = temp.resolve("profile");
        Files.write(path, portrait);
        when(workday.settings()).thenReturn(new WorkdayController.Settings("Asia/Kolkata", "09:00", "18:00", 60, List.of()));
        doReturn(List.of(new BirthdayController.Birthday(7, "Employee", "01-01", true, today,
            "/api/dashboard/birthdays/7/portrait")))
            .when(jdbc).query(anyString(), any(RowMapper.class), any(), any());
        doReturn(42L).when(jdbc).query(anyString(), any(ResultSetExtractor.class), any());
        when(files.find(42L, ProfileFileStore.Kind.PROFILE))
            .thenReturn(new ProfileFileStore.StoredFile(path, "image/jpeg"));
        var controller = new BirthdayController(jdbc, workday, files);

        MockMvcBuilders.standaloneSetup(controller).build()
            .perform(get("/api/dashboard/birthdays/7/portrait"))
            .andExpect(handler().handlerType(BirthdayController.class))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/jpeg"))
            .andExpect(content().bytes(portrait));
    }

    public static void main(String[] args) throws Exception {
        var test = new BirthdayPortraitRouteTest();
        test.temp = Files.createTempDirectory("birthday-portrait-route-");
        test.carouselPortraitUrlReturnsStoredImage();
    }
}
