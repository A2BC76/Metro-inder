package com.example.demo.controller;

import com.example.demo.service.TimeStationPersonnelService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
@AllArgsConstructor
public class TimeStationPersonnelController {
    TimeStationPersonnelService timeStationPersonnelService;
    @GetMapping("/seoulSubwayTimeZoneInformationSave")
    public void seoulSubwayTimeZoneInformationSave() throws IOException {
        timeStationPersonnelService.peopleInformationBySeoulAtTimeRead();
    }
}
