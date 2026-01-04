package communication;

import jssc.*;

public class SerialComm {

	SerialPort port;

	private boolean debug;  // Indicator of "debugging mode"
	
	// This function can be called to enable or disable "debugging mode"
	void setDebug(boolean mode) {
		debug = mode;
	}	
	

	// Constructor for the SerialComm class
	public SerialComm(String name) throws SerialPortException {
		port = new SerialPort(name);		
		port.openPort();
		port.setParams(SerialPort.BAUDRATE_9600,
			SerialPort.DATABITS_8,
			SerialPort.STOPBITS_1,
			SerialPort.PARITY_NONE);
		
		debug = false; // Default is to NOT be in debug mode
	}
		
	public void writeByte(byte b) {
        try {
            port.writeByte(b);
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
        if (debug) {
            System.out.println("(writeByte) Sent " + b + " <0x" + String.format("%02X", b) + ">");
        }
    }
 // readByte() method to read data from serial port
    public byte readByte() {
        byte b = -1;
        try {
            if (available()) {
                b = port.readBytes(1)[0];
                if (debug) {
                    //System.out.println("(readByte) Received " + b + " [0x" + String.format("%02X", b) + "]");
                    System.out.println( "[0x" + String.format("%02X", b) + "]");
                }
            }
        } catch (SerialPortException e) {
            e.printStackTrace();
        }
        return b;
    }
 // available() method to read data from serial port
    public boolean available() {
        try {
            return port.getInputBufferBytesCount() > 0;
        } catch (SerialPortException e) {
            e.printStackTrace();
            return false;
        }
    }

	
	
}
