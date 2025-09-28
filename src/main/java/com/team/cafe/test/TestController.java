package com.team.cafe.test;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@RequiredArgsConstructor
@Controller
public class TestController {

    private final TestRepository testRepository;

    @GetMapping("/test/list")
    public String list(Model model) {
        List<TestData> testList = this.testRepository.findAll();
        model.addAttribute("testList", testList);
        return "test_list";
    }
}


