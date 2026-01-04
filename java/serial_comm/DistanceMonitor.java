package serial_comm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

import jssc.*;

enum State {
	READ_MAGIC,
	READ_KEY,
	READ_TIMESTAMP_VALUE, //4 byte int
	READ_INFO_VALUE, //max 100 characters
	READ_ERROR_VALUE, //max 100 characters
	READ_POT_VALUE, //2 byte int
	READ_RAW_SENSOR_VALUE, //4 byte unsigned int in μs
	READ_ON_OFF_MSG, //
}

public class DistanceMonitor {
	// Our protocol magic number and keys.  Note, these must match those we defined in the Arduino!
	final byte MAGIC_NUMBER = 0x21;
	final byte INFO_UTF_KEY = 0x30;
	final byte ERROR_UTF_KEY = 0x31;
	final byte TIMESTAMP_KEY = 0x32;
	final byte POT_KEY = 0x33;
	final byte RAW_SENSOR_KEY = 0x34;
	final byte TOGGLE_KEY = 0x40;
	final byte DISP_MODE_KEY = 0x41;

	final private SerialComm port;
	
	public DisplayControl(String portname) throws SerialPortException {
		port = new SerialComm(portname);
	}

	public void run() throws SerialPortException, IOException {
		port.setDebug(false);
		
		State state = State.READ_MAGIC;
		int index = 0;
		long raw_sensor = 0;
		byte[] utf_b = new byte[100];
		long timestamp = 0;
		long pot_value = 0;
		long value = 0;
		float dist = 0;
		int len = 0;

		// Our main processing loop.
		while (true) {
			// Read and process a byte using non-blocking code. Is a byte available to read from the serial port?
			if (port.available()) {
				// Read the byte.  This is non-blocking because we know the byte is available!
				byte b = port.readByte();

				switch (state) {
				// Read the 1-byte header (i.e. the magic number).
				case READ_MAGIC:
					if (b == MAGIC_NUMBER) {
						System.out.println("Magic Number Read");
						state = State.READ_KEY;
					}
					break;
				// Read the key portion of the payload.
				case READ_KEY:
					// Interpret our protocol key.
					switch (b) {
					case TIMESTAMP_KEY:
						state = State.READ_TIMESTAMP_VALUE;
						// Initialize our state variables here every time!
						index = 0;
						value = 0;
						pot_value = 0;
						raw_sensor = 0;
						Arrays.fill(utf_b, (byte)0);
						timestamp = 0;
						dist = 0;
						System.out.println("Timestamp Key Read");
						break;
					case INFO_UTF_KEY:
						state = State.READ_INFO_VALUE;
						// Initialize our state variables here every time!
						index = 0;
						value = 0;
						pot_value = 0;
						raw_sensor = 0;
						Arrays.fill(utf_b, (byte)0);
						System.out.println("UTF Info Key Read");
						break;
					case ERROR_UTF_KEY:
						state = State.READ_ERROR_VALUE;
						index = 0;
						pot_value = 0;
						raw_sensor = 0;
						Arrays.fill(utf_b, (byte)0);
						System.out.println("UTF Error Key Read");
						break;
					case POT_KEY:
						state = State.READ_POT_VALUE;
						index = 0;
						value = 0;
						pot_value = 0;
						raw_sensor = 0;
						Arrays.fill(utf_b, (byte)0);
						System.out.println("Potentiometer Key Read");
						break;
					case RAW_SENSOR_KEY:
						state = State.READ_RAW_SENSOR_VALUE;
						index = 0;
						value = 0;
						pot_value = 0;
						raw_sensor = 0;
						Arrays.fill(utf_b, (byte)0);
						System.out.println("Sensor Key Read");
						break;
					default:
						System.out.println("***ERROR: Unknown key used " + String.format("%02X", b) + "***");
						state = State.READ_MAGIC;
						break;
					}
				break;
				// Read the timestamp value in our payload.
				case READ_TIMESTAMP_VALUE:
					value = (value << 8) | (b & 0xff);
					++index;
					if (index == 4) {
						// We've read all 4 bytes, so save the timestamp.  We will print it later.
						timestamp = value;
						System.out.println("Timestamp value is: " + timestamp);
						state = State.READ_MAGIC;
					}
				break;
				case READ_ERROR_VALUE:
					if(index <= 1) {
						len = (len << 8) | (b & 0xff);
						index++;
					}else {
						utf_b[index-2] = b;
						++index;
						if(b == '\0') {
							String t = new String(utf_b, StandardCharsets.UTF_8);
							System.out.println("Utf Error value is: " + t);
							state = State.READ_MAGIC;
						}
					}
				break;
				case READ_INFO_VALUE:
					if(index <= 1) {
						len = (len << 8) | (b & 0xff);
						index++;
					}else {
						utf_b[index-2] = b;
						++index;
						if(b == '\0') {
							String t = new String(utf_b, StandardCharsets.UTF_8);
							System.out.println("Utf Info value is: " + t);
							state = State.READ_MAGIC;
						}
					}
				break;
				case READ_POT_VALUE:
					value = (value << 8) | (b & 0xff);
					++index;
					if (index == 2) {
						// We've read all 4 bytes, so save the timestamp.  We will print it later.
						pot_value = value;
						System.out.println("Potentiometer value is: " + pot_value);
						state = State.READ_MAGIC;
					}
				break;
				case READ_RAW_SENSOR_VALUE:
					value = (value << 8) | (b & 0xff);
					++index;
					if (index == 4) {
						// We've read all 4 bytes, so re-compose the voltage value.  Then print the timestamp 
						// and voltage to the console.
						raw_sensor = value;
						dist = (float) (0.5 * raw_sensor * 0.0343);
						float precent = (dist/25) * 100;
						if(dist < 5) {
							System.out.println("Reading a distance of " + 0 + "cm, " + 0 + "% of range");
						}else if(dist > 25){
							System.out.println("Reading a distance of " + 25 + "cm, " + 100 + "% of range");
						}else {
							System.out.println("Reading a distance of " + dist + "cm, " + precent + "% of range");
						}
						
						state = State.READ_MAGIC;
					}
				break;
				default:
					System.out.println("Looking for magic key");
					state = State.READ_MAGIC;
				break;
				}
			}else if(System.in.available() > 0){
				byte msg = (byte)System.in.read();
				//if a key is entered that's D(0x3d44),L(0x3d4c),H(0x3d48),B(0x3d42), set the display mode to that
				if(msg == 'D' || msg == 'd') {
					port.writeByte(MAGIC_NUMBER);
					//write key
					port.writeByte(DISP_MODE_KEY);
					//write D
					byte lowByte = (byte)(0x3d44 >> 8);
					port.writeByte(lowByte);
					port.writeByte((byte)0x3d44);
				}else if(msg == 'L' || msg == 'l') {
					port.writeByte(MAGIC_NUMBER);
					//write key
					port.writeByte(DISP_MODE_KEY);
					//write L
					byte lowByte = (byte)(0x3d4c >> 8);
					port.writeByte(lowByte);
					port.writeByte((byte)0x3d4c);
				}else if(msg == 'H' || msg == 'h') {
					port.writeByte(MAGIC_NUMBER);
					//write key
					port.writeByte(DISP_MODE_KEY);
					//write H
					byte lowByte = (byte)(0x3d48 >> 8);
					port.writeByte(lowByte);
					port.writeByte((byte)0x3d48);	
				}else if(msg == 'B' || msg == 'b') {
					port.writeByte(MAGIC_NUMBER);
					//write key
					port.writeByte(DISP_MODE_KEY);
					//write B
					byte lowByte = (byte)(0x3d42 >> 8);
					port.writeByte(lowByte);
					port.writeByte((byte)0x3d42);	
				}
				//if a key is entered that reads as 0x3030 = ON (1 key), 0x3031 = OFF (0 key)
				else if(msg == '0') {
					port.writeByte(MAGIC_NUMBER);
					//write key
					port.writeByte(TOGGLE_KEY);
					//write 00
					byte lowByte = (byte)(0x3030 >> 8);
					port.writeByte(lowByte);
					port.writeByte((byte) 0x3030);		
				}else if(msg == '1') {
					port.writeByte(MAGIC_NUMBER);
					//write key
					port.writeByte(TOGGLE_KEY);
					//write 01
					byte lowByte = (byte)(0x3031 >> 8);
					port.writeByte(lowByte);
					port.writeByte((byte)(0x3031));		
				}
			}
		}
	}

	public static void main(String[] args) throws SerialPortException, IOException {
		DisplayControl dc = new DisplayControl("COM7"); // Adjust this to be the right port for your machine
		dc.run();
	}
}
