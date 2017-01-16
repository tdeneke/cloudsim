/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cloudbus.cloudsim.examples;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

/**
 *
 * @author tdeneke
 */
public class TranscodingCloudlet extends Cloudlet{
    /**
     * The predicted size of this Cloudlet to be executed in a CloudResource (unit: in MI).
     */
    private long predictedCloudletLength;
    private long frames;
    private long predictedWaitingLength;
    private double actualFps;
    private double predictedFps;

    public TranscodingCloudlet(int cloudletId, long cloudletLength, long predictedCloudletLength, long frames,int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.predictedCloudletLength = predictedCloudletLength;
        this.frames = frames;
        this.actualFps = (double)frames/cloudletLength;
        this.predictedFps = (double)frames/predictedCloudletLength;
    }
    
    /**
     * Sets the predicted length or size (in MI) of this Cloudlet to be executed in a CloudResource. This
     * Cloudlet length is calculated for 1 Pe only <tt>not</tt> the total length.
     * 
     * @param predictedCloudletLength the length or size (in MI) of this Cloudlet to be executed in a
     *            CloudResource
     * @return <tt>true</tt> if it is successful, <tt>false</tt> otherwise
     * @pre PredictedCloudletLength > 0
     * @post $none
     */
    public boolean setPredictedCloudletLength(final long predictedCloudletLength) {
        if (predictedCloudletLength <= 0) {
            return false;
        }
        this.predictedCloudletLength = predictedCloudletLength;
	return true;
    }
    
    /**
     * Gets the predicted length of this Cloudlet.
     * 
     * @return the predicted length of this Cloudlet
     * @pre $none
     * @post $result >= 0.0
     */
    public long getPredictedCloudletLength() {
        return predictedCloudletLength;
    }
    
    public boolean setFrames(final long frames) {
        if (frames <= 0) {
            return false;
        }
        this.frames = frames;
	return true;
    }
    
     public long getFrames() {
        return frames;
    }
    
    public boolean setPredictedWaitingLength(final long predictedWaitingLength) {
        if (predictedWaitingLength <= 0) {
            return false;
        }
        this.predictedWaitingLength = predictedWaitingLength;
	return true;
    }
    
    public long getPredictedWaitingLength() {
        return predictedWaitingLength;
    }
    
    public boolean setPredictedFps(final double predictedFps) {
        if (predictedFps <= 0) {
            return false;
        }
        this.predictedFps = predictedFps;
	return true;
    }
    
    public double getPredictedFps() {
        return predictedFps;
    }
     
    public boolean setActualFps(final double actualFps) {
        if (actualFps <= 0) {
            return false;
        }
        this.actualFps = actualFps;
	return true;
    }
    
    public double getActualFps() {
        return actualFps;
    }
}
