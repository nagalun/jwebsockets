package me.nagalun.jwebsockets.readers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.jenkov.nioserver.IEventHandler;
import com.jenkov.nioserver.Socket;
import com.jenkov.nioserver.memory.MemoryManager;

import me.nagalun.jwebsockets.FrameUtil;
import me.nagalun.jwebsockets.WebSocket;
import me.nagalun.jwebsockets.WebSocketServer;
import me.nagalun.jwebsockets.Protocol.Opcode;
import me.nagalun.jwebsockets.exceptions.InvalidFrameException;
import me.nagalun.jwebsockets.frames.FragmentedFrame;
import me.nagalun.jwebsockets.frames.Frame;
import me.nagalun.jwebsockets.frames.IFrame;

public final class WebsocketReader extends Reader {
	//private final IEventHandler evtHandler;
	private FragmentedFrame currentFragFrame;
	
	public WebsocketReader(final WebSocketServer server, final MemoryManager memoryManager, final IEventHandler evtHandler) {
		super(server, memoryManager);
		//this.evtHandler = evtHandler;
	}

	@Override
	public final void read(final Socket socket) throws IOException {
		if (readSock(socket) == -1) {
			return;
		}

		//final WebSocket ws = (WebSocket) socket.metaData;
		final ByteBuffer msgBuf = nextMessage.getBuffer();
		final long maxMsgSize = server.getMaxMessageSize();

		try {
			while (this.nextMessage.userData != 0) {
				final int frameLength = FrameUtil.isFrameComplete(msgBuf, maxMsgSize);
				if (frameLength != -1) {
					final Frame frame = Frame.fromBytes(msgBuf, true);
					if (!frame.FIN || (frame.opCode == Opcode.CONTINUATION && currentFragFrame != null)) {
						if (handleFragment(frame)) {
							handleFrame(currentFragFrame, socket);
							if (currentFragFrame != null) { /* Can happen if onMessage calls .close() */
								currentFragFrame.freeFrame();
								currentFragFrame = null;
							}
						}
					} else {
						handleFrame(frame, socket);
					}
					//evtHandler.onSocketMessage(socket, currentFrame.data);
					/* If less than total bytes read */
					if (!socket.endOfStreamReached && frameLength < this.nextMessage.userData) {
						msgBuf.position(frameLength);
						msgBuf.limit(this.nextMessage.userData);
						msgBuf.compact();
					} else if (socket.endOfStreamReached) {
						/* .close() already cleaned everything up, no need to endMessage() */
						return;
					}
					this.nextMessage.userData -= frameLength;
				} else {
					break;
				}
			}
			if (this.nextMessage.userData == 0) {
				endMessage();
			} else {
				nextMessage.resize(Math.min(nextMessage.userData * 2, (int) maxMsgSize));
			}
		} catch (final InvalidFrameException e) {
			socket.close();
		}
	}

	public void handleFrame(final IFrame frame, final Socket sock) {
		WebSocket ws = (WebSocket) sock.metaData;
		switch (frame.getOpcode()) {
		case TEXT:
			server.onMessage(ws, StandardCharsets.UTF_8.decode(frame.getBuffer()).toString());
			break;

		case BINARY:
			server.onMessage(ws, frame.getBuffer());
			break;
			
		case CLOSE: /* TODO: Doesn't conform to standard, FIX! */
			sock.close();
			break;
			
		case PING:
			System.out.println("Ping");
			break;
			
		/*case PONG:
			System.out.println("Pong");
			break;*/

		default: /* Unknown/Incorrect opcode */
			sock.close();
			break;
		}
	}
	
	public boolean handleFragment(final Frame frame) throws InvalidFrameException {
		if (currentFragFrame == null) {
			currentFragFrame = FragmentedFrame.fromFrame(frame, memoryManager);
			return false;
		} else {
			return currentFragFrame.appendFrame(frame, server.getMaxMessageSize());
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		if (currentFragFrame != null) {
			currentFragFrame.freeFrame();
			currentFragFrame = null;
		}
	}
}
