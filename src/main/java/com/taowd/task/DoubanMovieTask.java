package com.taowd.task;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.alibaba.fastjson.JSON;
import com.taowd.config.DouBanHttpGetUtil;
import com.taowd.config.HttpClient;
import com.taowd.pojo.DoubanBean;
import com.taowd.pojo.DoubanMovieBeanMySql;
import com.taowd.pojo.Movie;
import com.taowd.service.DoubanMovieService;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
public class DoubanMovieTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(DoubanMovieTask.class);
	private static final String URL = "https://movie.douban.com/j/new_search_subjects?sort=U&range=0,10&tags=&start=";

	@Autowired
	private DoubanMovieService doubanMovieService;

	@Autowired
	private JavaMailSender mailSender;

	@Value("${spring.mail.username}")
	private String username;

	// 发送邮件的模板引擎
	@Autowired
	private FreeMarkerConfigurer configurer;

	// @Scheduled(cron = "0/5 * * * * *")
	@Scheduled(cron = "* 10 22 ? * *")
	// 0 15 10 ? * *
	public void getMovieData() throws Exception {
		doubanMovieService.deleteMovieDetail();

		doubanMovieService.deleteAllData();

		Integer startIndex = 0;
		DoubanBean douban = getDoubanData(startIndex);

		while (douban.getData() != null && !douban.getData().isEmpty()) {
			try {
				douban = getDoubanData(startIndex);
				startIndex += 50;
				Thread.sleep(5000);
				System.out.println(String.format(startIndex + "--->" + JSON.toJSONString(douban)));
				if (!douban.getData().isEmpty()) {
					doubanMovieService.insertData(douban.getData());
				} else {
					continue;
				}

			} catch (Exception e) {
				LOGGER.error("发生异常：" + URL + startIndex, e);
			}

		}

		List<String> errorUrl = new ArrayList<String>();
		Integer resultUrl = 0;
		// 开始解析电影详情
		List<DoubanMovieBeanMySql> movieUrlList = doubanMovieService.selectAll();
		for (DoubanMovieBeanMySql item : movieUrlList) {
			try {
				Movie result = DouBanHttpGetUtil.extractMovie(item);
				// 数据入库
				doubanMovieService.insertMovieDetail(result);
				resultUrl++;

			} catch (Exception e) {
				LOGGER.error("详情入库发生异常", e);
				errorUrl.add(item.getUrl());
			}

		}

		// 邮件发送
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper message = new MimeMessageHelper(mimeMessage, true);
		message.setFrom(username);
		message.setTo("546642132@qq.com");
		message.setSubject(
				"【定时任务执行完毕-" + LocalDate.now() + " " + LocalTime.now().withNano(0) + "】");
		// message.setText("刷新影片总数：" + startIndex + "\n" + "获取详情信息：" + resultUrl);
		Map<String, Object> model = new HashMap<>();
		model.put("startIndex", startIndex);
		model.put("resultUrl", resultUrl);
		model.put("errorUrl", errorUrl);
		try {
			Template template = configurer.getConfiguration().getTemplate("message.ftl");
			try {
				String text = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
				message.setText(text, true);
				mailSender.send(mimeMessage);
			} catch (TemplateException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private DoubanBean getDoubanData(Integer start) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();

		HttpHeaders headers = new HttpHeaders(); // 设置请求发送方式
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // 将请求头部和参数合成一个请求

		String result = HttpClient.sendGetRequest(URL + start, params, headers);
		DoubanBean douban = JSON.parseObject(result, DoubanBean.class);

		return douban;
	}

}
