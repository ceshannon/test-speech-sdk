package com.lenovo.sdk.ms;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import com.alibaba.fastjson.JSON;

public class MicrosoftSpeechTest {

	private static final int ONE_HUNDRED_MILLISECOND = 3200;
	private static final String uri = "ws://10.120.115.183:8899/wss/translator";

	private static final Map<String, Object> request = new HashMap<>();
	
	private static final Path path = Paths.get("D:\\var\\music1.wav");
	// private static final Path path = Paths.get("D:\\var\\english1.wav");
	static {
		request.put("context", "test");
	}

	public static void sendData(WebSocketClient client) {
		byte[] data = new byte[0];
		try {
			data = Files.readAllBytes(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String header = JSON.toJSONString(request);
		System.out.println("send header: " + header);
		client.send(header);
		byte[] dest = new byte[ONE_HUNDRED_MILLISECOND];
		for (int i = 0; i * ONE_HUNDRED_MILLISECOND < data.length; i++) {
			Arrays.fill(dest, (byte) 0);
			System.arraycopy(data, i * ONE_HUNDRED_MILLISECOND, dest, 0,
					Math.min(ONE_HUNDRED_MILLISECOND, data.length - i * ONE_HUNDRED_MILLISECOND));

			System.out.println("send data:" + i);
			client.send(dest);
			try {
				Thread.sleep(RandomUtils.nextLong(100, 200));
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		client.send("".getBytes());

	}

	public static void main(String[] args) throws URISyntaxException, IOException {
		wsConnect();
	}

	private static void wsConnect() throws URISyntaxException, FileNotFoundException {
		final WebSocketClient webSocketClient = new WebSocketClient(new URI(uri), new Draft_6455()) {
			WebSocketClient that = this;

			@Override
			public void onOpen(ServerHandshake serverHandshake) {
				System.out.println("websocket open");
				Runnable r = new Runnable() {
					@Override
					public void run() {
						sendData(that);
					}
				};
				new Thread(r).start();

			}

			@Override
			public void onMessage(String s) {
				System.out.println("on message:" + s);
			}

			public void onMessage(ByteBuffer bytes) {
				try {
					byte[] b = bytes.array();
					boolean frameFinalFlag = (b[0] & 0x80) != 0;
					System.out.println("==is final binary:" + frameFinalFlag);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("on binary:" + bytes.array().length);

			};

			@Override
			public void onClose(int i, String s, boolean b) {
				System.out.println("on close:" + s);
			}

			@Override
			public void onError(Exception e) {
				System.out.println("on error:" + e);
			}
		};
		webSocketClient.connect();
	}
}
