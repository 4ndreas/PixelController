/**
 * Copyright (C) 2011-2013 Michael Vogt <michu@neophob.com>
 *
 * This file is part of PixelController.
 *
 * PixelController is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PixelController is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PixelController.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.neophob.sematrix.output;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.neophob.sematrix.glue.Collector;
import com.neophob.sematrix.output.gamma.GammaType;
import com.neophob.sematrix.properties.ApplicationConfigurationHelper;
import com.neophob.sematrix.properties.ColorFormat;
import com.neophob.sematrix.properties.DeviceConfig;

/**
 * 
 * Send data to a TPM2Net Device.
 * 
 * TPM2 use UDP as transport layer, Port 65506
 * 
 * see http://www.ledstyles.de/ftopic18969.html for more details
 * 
 * Protocol:
 * Blockstart-Byte:        0x9C
 * 
 * Block-Art:              0xDA = Datenframe (DAta) *oder*
 *                         0xC0 = Befehl (Command) *oder*
 *                         0xAA = Angeforderte Antwort (vom Datenempfänger an den Sender)
 * 
 * Framegrösse in 16 Bit:  High-Byte zuerst, dann
 *                         Low-Byte
 * 
 * Paketnummer:            0-255
 * 
 * Nutzdaten:              1 - 65.535 Bytes Daten oder Befehle mit Parametern
 * 
 * Blockende-Byte:         0x36
 * 
 * 
 * 
 * @author michu
 *
 */
public class Tpm2Net extends Output {
	
	/** The log. */
	private static final Logger LOG = Logger.getLogger(Tpm2Net.class.getName());

	private static final int TPM2_NET_HEADER_SIZE = 5;
	private static final int TPM2_NET_PORT = 65506;
	
	private DatagramSocket outputSocket;
	private InetAddress targetAddr;
	
	/** The initialized. */
	protected boolean initialized;

	/** The display options, does the buffer needs to be flipped? rotated? */
	private List<DeviceConfig> displayOptions;
	
	/** The output color format. */
	private List<ColorFormat> colorFormat;
	
	/** define how the panels are arranged */
	private List<Integer> panelOrder;

	   /** The x size. */
	 protected int xResolution;
	 
	 /** The y size. */
	 protected int yResolution;
	
	private DatagramPacket tpm2UdpPacket;

    /** flip each 2nd scanline? */
    protected boolean snakeCabeling;
    
    /** Manual mapping */
    protected int[] mapping;
	
    /** does the image needs to be rotated?*/
    protected DeviceConfig displayOption;
    
    
    
	private String targetAddrStr;
	
	private long errorCounter = 0;
	
	private boolean applyGamma = false;
	
	/**
	 * 
	 * @param ph
	 * @param controller
	 */
	public Tpm2Net(ApplicationConfigurationHelper ph, PixelControllerOutput controller) {
		super(OutputDeviceEnum.TPM2NET, ph, controller, 8);
		
		this.displayOptions = ph.getTpm2NetDevice();		
		this.colorFormat = ph.getColorFormat();
		this.panelOrder = ph.getPanelOrder();
		
        this.xResolution = ph.parseOutputXResolution();
        this.yResolution = ph.parseOutputYResolution();
        this.snakeCabeling = ph.isOutputSnakeCabeling();
        this.mapping = ph.getOutputMappingValues();
		
        
        //get the mini dmx layout
        this.displayOption = ph.getOutputDeviceLayout();     
        if (this.displayOption==null) {
            this.displayOption = DeviceConfig.NO_ROTATE;
        }
        
        if(ph.getGammaType() != GammaType.NONE )
        {
        	applyGamma = true;
        }
        	
		targetAddrStr = ph.getTpm2NetIpAddress();
		this.initialized = false;		

		try {
			this.targetAddr = InetAddress.getByName(targetAddrStr);			
			this.outputSocket = new DatagramSocket();
			this.tpm2UdpPacket = new DatagramPacket(new byte[0], 0, targetAddr, TPM2_NET_PORT);

			this.initialized = true;
			LOG.log(Level.INFO, "Initialized TPM2NET device, target IP: {0}, Resolution: {1}/{2}",  
					new Object[] { this.targetAddr.toString(), this.matrixData.getDeviceXSize(), this.matrixData.getDeviceYSize()}
			);

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Failed to resolve target address: {0}", e);
		}
	}

	
	/**
	 * send out a tpm2net paket
	 *  
	 * @param packetNumber: a tpm2net frame can consists out of multiple udp packets
	 *                      HINT: this violate the tpm2net protocol!
	 *                      TODO 
	 * @param frameSize
	 * @param data
	 */
	private void sendTpm2NetPacketOut(int packetNumber, byte[] data) {
		int frameSize = data.length;
        byte[] outputBuffer = new byte[frameSize + TPM2_NET_HEADER_SIZE + 1];
        
        //write header of udp packet
		outputBuffer[0] = (byte)0x9c;
		outputBuffer[1] = (byte)0xda;
		outputBuffer[2] = ((byte)(frameSize >> 8 & 0xFF));
		outputBuffer[3] = ((byte)(frameSize & 0xFF));
		outputBuffer[4] = ((byte)packetNumber);
		
		//write footer
		outputBuffer[TPM2_NET_HEADER_SIZE + frameSize] = (byte)0x36;		
		
		//copy payload
		System.arraycopy(data, 0, outputBuffer, TPM2_NET_HEADER_SIZE, frameSize);		
		
		tpm2UdpPacket.setData(outputBuffer);

		try {
			this.outputSocket.send(tpm2UdpPacket);
		} catch (Exception e) {
		    errorCounter++;
			LOG.log(Level.SEVERE, "Failed to send network data: {0}", e);
		}
	}

	@Override
	/**
	 * update panels
	 */
	public void update() {

		if (initialized) {			
			for (int ofs=0; ofs<Collector.getInstance().getNrOfScreens(); ofs++) {
				//get the effective panel buffer
				int panelNr = this.panelOrder.get(ofs);

				int[] transformedBuffer = 
						RotateBuffer.transformImage(super.getBufferForScreen(ofs,applyGamma), displayOptions.get(panelNr),
								this.matrixData.getDeviceXSize(), this.matrixData.getDeviceYSize());

				
		        if (this.snakeCabeling) {
		            //flip each 2nd scanline
		            transformedBuffer= OutputHelper.flipSecondScanline(transformedBuffer, xResolution, yResolution);
		        } else if (this.mapping.length>0) {
		        	//do manual mapping
		        	transformedBuffer = OutputHelper.manualMapping(transformedBuffer, mapping, xResolution, yResolution);
		        }
		        
				
				byte[] rgbBuffer = OutputHelper.convertBufferTo24bit(transformedBuffer, colorFormat.get(panelNr));
				
				//send small UDP packages, this is not optimal but the client needs less memory
				//TODO maybe add option to send one or mutiple packets
				sendTpm2NetPacketOut(ofs, rgbBuffer);
			}
	
			
			int doorX = 2;
			int doorY = 12;
			int panelNr = 1;
			
			int[] transformedBuffer = 
					RotateBuffer.transformImage(super.getBufferForScreen(panelNr,applyGamma), displayOptions.get(panelNr),
							doorX,doorY);

			transformedBuffer= OutputHelper.flipSecondScanline(transformedBuffer, doorX, doorY);

			byte[] rgbBuffer = OutputHelper.convertBufferTo24bit(transformedBuffer, colorFormat.get(panelNr));
			
			//send small UDP packages, this is not optimal but the client needs less memory
			//TODO maybe add option to send one or mutiple packets
			sendTpm2NetPacketOut(2, rgbBuffer);
			
			
			
		}
	}

    @Override
    public boolean isSupportConnectionState() {
        return true;
    }

    @Override
    public boolean isConnected() {
        return initialized;
    }
    
    @Override
    public String getConnectionStatus(){
        if (initialized) {
            return "Target IP "+targetAddrStr+":"+TPM2_NET_PORT;            
        }
        return "Not connected!";
    }
	    
    @Override
    public long getErrorCounter() {        
        return errorCounter;
    }
    
	@Override
	public void close() {		
		if (this.initialized) {
			LOG.log(Level.INFO, "Close network socket");
			try {
				this.outputSocket.close();				
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Failed to close network socket: {0}", e);
			}
		} else {
			LOG.log(Level.INFO, "Network socket not initialized, nothing to do.");
		}		
	}
	
}