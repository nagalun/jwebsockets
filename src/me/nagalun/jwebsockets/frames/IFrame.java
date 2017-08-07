/**
 * 
 */
package me.nagalun.jwebsockets.frames;

import java.nio.ByteBuffer;

import me.nagalun.jwebsockets.Protocol.Opcode;

/**
 * @author nagalun
 * @date 07-08-2017
 */
public interface IFrame {
	public ByteBuffer getBuffer();
	public Opcode getOpcode();
}
