package com.example.springbootnetty.common;

import com.example.springbootnetty.entity.UserInfo;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private Logger loger = LogManager.getLogger();
	private final String webUri;
	private final String INDEX = "classpath:main/webapp/index.jsp";
	
	public HttpRequestHandler(String webUri) {
		this.webUri = webUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		loger.info("===========> {}, {}", webUri, request.uri());
		
		String uri = StringUtils.substringBefore(request.uri(), "?");
		if(webUri.equalsIgnoreCase(uri)) {//获取webSocket参数
			QueryStringDecoder query = new QueryStringDecoder(request.uri());
			Map<String, List<String>> map = query.parameters();
			List<String> tokens = map.get("token");
			
			//根据参数保存当前登录对象, 并把该token加入到channel中
			if(tokens != null && !tokens.isEmpty()) {
				String token = tokens.get(0);
				ChatConstants.addOnlines(token, new UserInfo(token));
				ctx.channel().attr(ChatConstants.CHANNEL_TOKEN_KEY).getAndSet(token);
			}
			
			request.setUri(uri);
			ctx.fireChannelRead(request.retain());
		}else {
			if(HttpUtil.is100ContinueExpected(request)) {
				send100ContinueExpected(ctx);
			}
			
			RandomAccessFile file = new RandomAccessFile(INDEX, "r");
			HttpResponse response = new DefaultHttpResponse(request.protocolVersion(), HttpResponseStatus.OK);
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
			
			boolean keepAlive = HttpUtil.isKeepAlive(request);
			if(keepAlive) {
				response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
				response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			}
			ctx.write(response);
			
			if(ctx.pipeline().get(SslHandler.class) == null) {
				ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()));
			}else {
				ctx.write(new ChunkedNioFile(file.getChannel()));
			}
			
			ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
			if(!keepAlive) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
			
			file.close();
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
		ctx.close();
	}
	
	private void send100ContinueExpected(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONFLICT);
		ctx.writeAndFlush(response);		
	}
}
