package cn.whyyu.cvserver.controller;

import cn.whyyu.cvserver.util.CommonResult;
import cn.whyyu.cvserver.video.config.VideoConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video")
public class VideoController {
    @Autowired
    private VideoConfig videoConfig;

    @RequestMapping("/getVideo")
    public CommonResult<String> getVideo(@RequestParam String videoId) {

        return CommonResult.success(videoConfig.getNginxUrl() + videoId);
    }


}
