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

    public TranscodingCloudlet(int cloudletId, long cloudletLength, long predictedCloudletLength, int pesNumber, long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw) {
        super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu, utilizationModelRam, utilizationModelBw);
        this.predictedCloudletLength = predictedCloudletLength;
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
}
