package com.example.springbootnetty;

import com.example.springbootnetty.common.ChatServer;
import io.netty.channel.ChannelFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;

@SpringBootApplication
public class SpringBootNettyApplication implements CommandLineRunner {
	@Autowired
	private ChatServer chatServer;
	public static void main(String[] args) {
		SpringApplication.run(SpringBootNettyApplication.class, args);
	}

	@Bean
	public ChatServer chatServer() {
		return new ChatServer();
	}
	@Override
	public void run(String... strings) throws Exception {
		InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9090);
		ChannelFuture future = chatServer.start(address);

		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				chatServer.destroy();
			}
		});

		future.channel().closeFuture().syncUninterruptibly();
	}
}
