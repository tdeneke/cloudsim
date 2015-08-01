package org.cloudbus.cloudsim.examples;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.CloudletScheduler;

public class TranscodingVm extends Vm {

    private double startTime;
    private double remainingTime;
    private boolean closeToComplete = false;
    private double rentingTime;
    private double cost;

    public TranscodingVm(int id, int userId, int mips, int numberOfPes, int ram, long bw, long size, String vmm, CloudletScheduler cloudletScheduler, double startTime, double rentingTime) {
        super(id, userId, mips, numberOfPes, ram, bw, size, vmm, cloudletScheduler);
        // TODO Auto-generated constructor stub
        this.startTime = startTime;
        this.closeToComplete = false;
        this.rentingTime = rentingTime;

    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public void setRemainingTime(double remainingTime) {
        this.remainingTime = remainingTime;
    }

    public double getRemainingTime() {
        return remainingTime;
    }
    
    public void setRentingTime(double rentingTime) {
        this.rentingTime = remainingTime;
    }

    public double getRentingTime() {
        return rentingTime;
    }

    public void setCloseToComplete(boolean closeToComplete) {
        this.closeToComplete = closeToComplete;
    }

    public boolean getCloseToComplete() {
        return closeToComplete;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

}
