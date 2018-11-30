package com.lenovo.speech;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TranslatorWebsocketServer extends WebSocketServer implements TranslatorDataCallback, CommandLineRunner{
	private final Map<String, WebSocket> taskSession = new ConcurrentHashMap<>();
	
	public TranslatorWebsocketServer() {
		super(new InetSocketAddress(8899));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		Iterator<String> it = handshake.iterateHttpFields();
		while (it.hasNext()) {
			String string = (String) it.next();
			System.out.println(handshake.getFieldValue(string));
		}
		conn.setAttachment(UUID.randomUUID().toString());
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		String cid = conn.<String>getAttachment();
		taskSession.remove(cid);
		TranslationTask task = TranslationTaskMgmt.find(cid);
		task.destroy();
		TranslationTaskMgmt.unregister(cid);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		String cid = conn.<String>getAttachment();
		TranslationTask task = createTask(cid);
		new Thread(task).start();
		TranslationTaskMgmt.register(cid, task);
		taskSession.put(cid, conn);
	}
	
	private TranslationTask createTask(String sid) {
		return new MicroSoftTranslationTask(sid, this);
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		if (message != null) {
			byte[] data = message.array();
			String cid = conn.<String>getAttachment();
			TranslationTask task = TranslationTaskMgmt.find(cid);
			task.enqueue(data);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
		if (conn != null) {
			String cid = conn.<String>getAttachment();
			taskSession.remove(cid);
			TranslationTask task = TranslationTaskMgmt.find(cid);
			task.destroy();
			TranslationTaskMgmt.unregister(cid);
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
	}

	@Override
	public void onResult(String sid, String data) {
		System.out.println("result:" + data);
		WebSocket session = taskSession.get(sid);
		if (session != null) {
			try {
				session.send(data);
			} catch (Exception e) {
				System.out.println("websocket handler exception:" + data);
			}
		}
	}

	@Override
	public void onResult(String sid, byte[] data) {
		
	}

	@Override
	public void onClose(String sid) {
		WebSocket session = taskSession.get(sid);
		if (session != null) {
			try {
				session.close();
			} catch (Exception e) {
			}
		}
		taskSession.remove(sid);
//		TranslationTaskMgmt.unregister(sid);
	}
	
	public static void main(String[] args) {
		SpringApplication.run(TranslatorWebsocketServer.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		new TranslatorWebsocketServer().start();
	}

}
