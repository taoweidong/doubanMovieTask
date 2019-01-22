package com.taowd.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.alibaba.fastjson.JSON;
import com.taowd.config.DouBanHttpGetUtil;
import com.taowd.config.HttpClient;
import com.taowd.pojo.DoubanBean;
import com.taowd.pojo.DoubanMovieBeanMySql;
import com.taowd.pojo.Movie;
import com.taowd.service.DoubanMovieService;

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

	// @Scheduled(cron = "0/5 * * * * *")
	@Scheduled(cron = "* 50 23 ? * *")
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
					break;
				}

			} catch (Exception e) {
				System.out.println("发生异常：" + URL + startIndex);
			}

		}

		// 开始解析电影详情
		List<DoubanMovieBeanMySql> movieUrlList = doubanMovieService.selectAll();
		for (DoubanMovieBeanMySql item : movieUrlList) {
			try {
				Movie result = DouBanHttpGetUtil.extractMovie(item);
				// 数据入库
				doubanMovieService.insertMovieDetail(result);
			} catch (Exception e) {
				LOGGER.error("详情入库发生异常", e);
			}

		}

		// 邮件发送
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(username);
		message.setTo("546642132@qq.com");
		message.setSubject("主题：定时任务执行完毕");
		message.setText("处理总数：" + startIndex + "\n" + "详情获取成功");
		mailSender.send(message);

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
