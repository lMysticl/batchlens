package io.github.lmysticl.batchlens.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public final class BatchPage {
  @GetMapping({"/batchlens", "/batchlens/"})
  public String index() {
    return "redirect:/batchlens/index.html";
  }
}
