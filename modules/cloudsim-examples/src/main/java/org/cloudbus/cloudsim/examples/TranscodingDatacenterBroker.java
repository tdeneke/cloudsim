/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.examples;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;

/**
 * DatacentreBroker represents a broker acting on behalf of a user. It hides VM
 * management, as vm creation, sumbission of cloudlets to this VMs and
 * destruction of VMs.
 *
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class TranscodingDatacenterBroker extends SimEntity {

    /**
     * The vm list.
     */
    protected List<? extends Vm> vmList;

    /**
     * The vms created list.
     */
    protected List<? extends Vm> vmsCreatedList;

    /**
     * The cloudlet list.
     */
    protected List<? extends Cloudlet> cloudletList;

    /**
     * The cloudlet submitted list.
     */
    protected List<? extends Cloudlet> cloudletSubmittedList;

    /**
     * The cloudlet received list.
     */
    protected List<? extends Cloudlet> cloudletReceivedList;

    /**
     * The cloudlets submitted.
     */
    protected int cloudletsSubmitted;

    /**
     * The vms requested.
     */
    protected int vmsRequested;

    /**
     * The vms acks.
     */
    protected int vmsAcks;

    /**
     * The vms destroyed.
     */
    protected int vmsDestroyed;

    /**
     * The datacenter ids list.
     */
    protected List<Integer> datacenterIdsList;

    /**
     * The datacenter requested ids list.
     */
    protected List<Integer> datacenterRequestedIdsList;

    /**
     * The vms to datacenters map.
     */
    protected Map<Integer, Integer> vmsToDatacentersMap;

    /**
     * The datacenter characteristics list.
     */
    protected Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList;

    private static final int VM_CREATE_REQ = 200;
    private static final int VM_DESTROY_REQ = 201;
    private static final int CLOUDLET_SUBMIT_REQ = 202;
    private static final long MIPS = 124162;
    private static final int BASE_NO_VMS = 2000;
    private static final double SLA_WAITING_TIME = 3*60;
    private static final int MIN_INTERVAL = 9 * 60;
    private static final double RENTING_TIME = 60*60;
    private static final double UPPER_THRESHOLD = 15*60;
    private static final double LOWER_THRESHOLD = 5*60; 
    protected Map<Integer, Integer> VmsToJobCountMap;
    protected Map<Integer, Long> VmsToJobLoadMap;
    protected BufferedWriter bw;
    protected String lbAlgo;
    protected int vmId;
    protected int cloudletId;
    protected double lastModifiedAt;
    protected boolean endOfSimulation;

    /**
     * Created a new DatacenterBroker object.
     *
     * @param name name to be associated with this entity (as required by
     * Sim_entity class from simjava package)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public TranscodingDatacenterBroker(String name) throws Exception {
        super(name);

        setVmList(new ArrayList<Vm>());
        setVmsCreatedList(new ArrayList<Vm>());
        setCloudletList(new ArrayList<Cloudlet>());
        setCloudletSubmittedList(new ArrayList<Cloudlet>());
        setCloudletReceivedList(new ArrayList<Cloudlet>());

        cloudletsSubmitted = 0;
        setVmsRequested(0);
        setVmsAcks(0);
        setVmsDestroyed(0);

        setDatacenterIdsList(new LinkedList<Integer>());
        setDatacenterRequestedIdsList(new ArrayList<Integer>());
        setVmsToDatacentersMap(new HashMap<Integer, Integer>());
        setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());
        setVmsToJobCountMap(new HashMap<Integer, Integer>());
        setVmsToJobLoadMap(new HashMap<Integer, Long>());
        setLbAlgo("qlen");
        setEndOfSimulation(false);

        vmId = 0;
        cloudletId = 0;
        lastModifiedAt = CloudSim.clock();
    }

    private static List<TranscodingVm> createVM(int userId, int vms, int idShift) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<TranscodingVm> list = new LinkedList<TranscodingVm>();

        //VM Parameters
        long size = 1000; //image size (MB)
        int ram = 512; //vm memory (MB)
        int mips = 124162; //approximate for a real core
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        TranscodingVm[] vm = new TranscodingVm[vms];

        for (int i = 0; i < vms; i++) {
            vm[i] = new TranscodingVm(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared(), CloudSim.clock(), RENTING_TIME);
            list.add(vm[i]);
        }

        return list;
    }

    /**
     * This method is used to send to the broker the list with virtual machines
     * that must be created.
     *
     * @param list the list
     * @pre list !=null
     * @post $none
     */
    public void submitVmList(List<? extends Vm> list) {
        getVmList().addAll(list);
    }

    /**
     * This method is used to send to the broker the list of cloudlets.
     *
     * @param list the list
     * @pre list !=null
     * @post $none
     */
    public void submitCloudletList(List<? extends Cloudlet> list) {
        getCloudletList().addAll(list);
    }

    /**
     * Specifies that a given cloudlet must run in a specific virtual machine.
     *
     * @param cloudletId ID of the cloudlet being bount to a vm
     * @param vmId the vm id
     * @pre cloudletId > 0
     * @pre id > 0
     * @post $none
     */
    public void bindCloudletToVm(int cloudletId, int vmId) {
        CloudletList.getById(getCloudletList(), cloudletId).setVmId(vmId);
    }

    /**
     * Processes events available for this Broker.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // Resource characteristics request
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                processResourceCharacteristicsRequest(ev);
                break;
            // Resource characteristics answer
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                processResourceCharacteristics(ev);
                break;
            // VM Creation request 
            case VM_CREATE_REQ:
                //processVmCreateReq(ev);
                createVmsInDatacenter(getDatacenterIdsList().get(0));
                break;
            // VM Creation answer
            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev);
                break;
            // VM Destroy request 
            case VM_DESTROY_REQ:
                destroyVmsInDatacenter(getDatacenterIdsList().get(0));
                break;
            // VM Destroy answer
            case CloudSimTags.VM_DESTROY_ACK:
                processVmDestroy(ev);
                break;
            case CLOUDLET_SUBMIT_REQ:
                submitCloudlets();
                break;
            // A finished cloudlet returned
            case CloudSimTags.CLOUDLET_RETURN:
                processCloudletReturn(ev);
                break;
            // if the simulation finishes
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    /**
     * Process the return of a request for the characteristics of a
     * PowerDatacenter.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processResourceCharacteristics(SimEvent ev) {
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            setDatacenterRequestedIdsList(new ArrayList<Integer>());
            //createVmsInDatacenter(getDatacenterIdsList().get(0));
        }
    }

    /**
     * Process a request for the characteristics of a PowerDatacenter.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processResourceCharacteristicsRequest(SimEvent ev) {
        setDatacenterIdsList(CloudSim.getCloudResourceList());
        setDatacenterCharacteristicsList(new HashMap<Integer, DatacenterCharacteristics>());

        Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloud Resource List received with ",
                getDatacenterIdsList().size(), " resource(s)");

        for (Integer datacenterId : getDatacenterIdsList()) {
            sendNow(datacenterId, CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
        }
    }

    /**
     * Process the ack received due to a request for VM creation.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processVmCreate(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().put(vmId, datacenterId);
            getVmsCreatedList().add(VmList.getById(getVmList(), vmId));
            getVmsToJobCountMap().put(vmId, 0);
            getVmsToJobLoadMap().put(vmId, 0L);
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": VM #", vmId,
                    " has been created in Datacenter #", datacenterId);
            ((TranscodingVm) VmList.getById(getVmsCreatedList(), vmId)).setStartTime(CloudSim.clock());
            lastModifiedAt = CloudSim.clock();
        } else {
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Creation of VM #", vmId,
                    " failed in Datacenter #", datacenterId);
        }
        incrementVmsAcks();
    }

    /**
     * Process the ack received due to a request for VM destroy.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processVmDestroy(SimEvent ev) {
        int[] data = (int[]) ev.getData();
        int datacenterId = data[0];
        int vmId = data[1];
        int result = data[2];

        if (result == CloudSimTags.TRUE) {
            getVmsToDatacentersMap().remove(vmId);
            getVmsCreatedList().remove(VmList.getById(getVmList(), vmId));
            getVmsToJobCountMap().remove(vmId);
            getVmsToJobLoadMap().remove(vmId);
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": VM #", vmId,
                    " has been removed from Datacenter #", datacenterId);
            lastModifiedAt = CloudSim.clock();
        } else {
            Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Termination of VM #", vmId,
                    " failed in Datacenter #", datacenterId);
        }

        //((TranscodingVm)VmList.getById(getVmsCreatedList(), vmId)).setStartTime(CloudSim.clock());
        //incrementVmsAcks();
    }

    /**
     * Process a cloudlet return event.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processCloudletReturn(SimEvent ev) {
        TranscodingCloudlet cloudlet = (TranscodingCloudlet) ev.getData();
        getCloudletReceivedList().add(cloudlet);
        Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Cloudlet ", cloudlet.getCloudletId()," received");
        getVmsToJobCountMap().put(cloudlet.getVmId(), getVmsToJobCountMap().get(cloudlet.getVmId()) - 1); 
        getVmsToJobLoadMap().put(cloudlet.getVmId(), getVmsToJobLoadMap().get(cloudlet.getVmId()) - cloudlet.getPredictedCloudletLength());
        cloudletsSubmitted--;  
    }

    /**
     * Overrides this method when making a new and different type of Broker.
     * This method is called by {@link #body()} for incoming unknown tags.
     *
     * @param ev a SimEvent object
     * @pre ev != null
     * @post $none
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printConcatLine(getName(), ".processOtherEvent(): ", "Error - an event is null.");
            return;
        }

        Log.printConcatLine(getName(), ".processOtherEvent(): Error - event unknown by this DatacenterBroker.");
    }

    /**
     * Create the virtual machines in a datacenter.
     *
     * @param datacenterId Id of the chosen PowerDatacenter
     * @pre $none
     * @post $none
     */
    protected void createVmsInDatacenter(int datacenterId) {

        try {
            
            // if end of simulation stop scheduling 
            if(getEndOfSimulation())
                return;
            
            // request additional or removal of n VMs
            int requestedVms = 0;
            String datacenterName = CloudSim.getEntityName(datacenterId);
            int vmsToRequest = calculateProvision(getResultWriter());

            // removal, just schedule next vm creation
            if (vmsToRequest <= 0) {
                schedule(getId(), 180, VM_CREATE_REQ);
                return;
            }

            // if not recently modified
            if ((CloudSim.clock() - lastModifiedAt) > MIN_INTERVAL) {
                
                // unflag vms instead of creating new; previously flagged vms 
                // does not get new jobs as they are candidates for removal
                for (Vm vm : getVmsCreatedList()) {
                    if (vmsToRequest == 0) {
                        break;
                    }
                    if (((TranscodingVm) vm).getCloseToComplete()) {
                        ((TranscodingVm) vm).setCloseToComplete(false);
                        vmsToRequest--;
                    }
                }

                // create more new vms if needed
                for (Vm vm : createVM(getId(), vmsToRequest, vmId)) {
                    if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                        Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
                                + " in " + datacenterName);
                        sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                        getVmList().add(vm);
                        requestedVms++;
                    }
                }

            }
            
            vmId = vmId + requestedVms;
            getDatacenterRequestedIdsList().add(datacenterId);
            setVmsRequested(getVmsRequested() + requestedVms);
            schedule(getId(), 180, VM_CREATE_REQ);

        } catch (IOException ex) {
            Logger.getLogger(TranscodingDatacenterBroker.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Problem writing results");
        }
    }

    /**
     * Destroy virtual machines in a datacenter.
     *
     * @param datacenterId Id of the chosen PowerDatacenter
     * @pre $none
     * @post $none
     */
    protected void destroyVmsInDatacenter(int datacenterId) {

        try {
            
            // if end of simulation stop scheduling 
            if(getEndOfSimulation())
                return;
            
            // request n additional VMs
            String datacenterName = CloudSim.getEntityName(datacenterId); 
            int vmsToDestroy = calculateProvision(getResultWriter());

            // we expect negative value for vmsToDestroy; posotive vmsToDestroy
            // means we need to create more.
            if (vmsToDestroy >= 0) {
                schedule(getId(), 180, VM_DESTROY_REQ);
                return;
            }

            // if not modified recently 
            if ((CloudSim.clock() - lastModifiedAt) > MIN_INTERVAL) {
                
                // destroy vms which are flagd and have no job queued
                for (Vm vm : getVmsCreatedList()) {
                    if (vmsToDestroy == 0) {
                        break;
                    }
                    System.out.println("about to destroy vm");
                    if (((TranscodingVm) vm).getCloseToComplete() && getVmsToJobCountMap().get(vm.getId()) == 0) {
                        Log.printConcatLine(CloudSim.clock(), ": " + getName(), ": Destroying VM #", vm.getId(), "in datacenter " + datacenterName);
                        sendNow(datacenterId, CloudSimTags.VM_DESTROY_ACK, vm);
                        vmsToDestroy ++;
                    }
                }
                
                // flag the rest; flagged vms don't get new jobs and are 
                // candidates to be removed once they finished their job or become 
                // unflgged. 
                for (Vm vm : getVmsCreatedList()) {
                    if (vmsToDestroy == 0) {
                        break;
                    }
                    ((TranscodingVm)vm).setRemainingTime( ((TranscodingVm) vm).getRentingTime() - ((CloudSim.clock() - ((TranscodingVm) vm).getStartTime()) %  ((TranscodingVm) vm).getRentingTime()));
                    if (  LOWER_THRESHOLD < (int)(((TranscodingVm)vm).getRemainingTime()) && (int)(((TranscodingVm)vm).getRemainingTime()) < UPPER_THRESHOLD) {
                        ((TranscodingVm) vm).setCloseToComplete(true);
                        System.out.println("flaged vm!");
                        vmsToDestroy ++;
                    }

                }

            }

            getDatacenterRequestedIdsList().add(datacenterId);

            //setVmsRequested(requestedVms);
            //setVmsAcks(0);
            //setVmsRequested(getVmsRequested() + requestedVms);
            schedule(getId(), 180, VM_DESTROY_REQ);
        } catch (IOException ex) {
            Logger.getLogger(TranscodingDatacenterBroker.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Problem writing results");
        }
    }

    /**
     * Submit cloudlets to the created VMs.
     *
     * @pre $none
     * @post $none
     */
    protected void submitCloudlets() {
        
        // if end of simulation stop scheduling 
        if(getEndOfSimulation())
            return;
        
        if (getVmsCreatedList().size() == 0) {
            schedule(getId(), 5, CLOUDLET_SUBMIT_REQ);
            return;
        }

        int vmIndex = getBestVmIndex();
        for (Cloudlet cld : getCloudletList()) {
            TranscodingCloudlet cloudlet = (TranscodingCloudlet) cld; //here type casting required or returns as type of parent class
            Vm vm;
            // if user didn't bind this cloudlet and it has not been executed yet
            if (cloudlet.getVmId() == -1) {
                vm = getVmsCreatedList().get(vmIndex);
            } else { // submit to the specific vm
                vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
                if (vm == null) { // vm was not created
                    Log.printLine(CloudSim.clock() + ": " + getName() + ": Postponing execution of cloudlet "
                            + cloudlet.getCloudletId() + ": bount VM not available");
                    continue;
                }
            }

            Log.printLine(CloudSim.clock() + ": " + getName() + ": Sending cloudlet "
                    + cloudlet.getCloudletId() + " to VM #" + vm.getId());
            cloudlet.setVmId(vm.getId());

            //cloudlet.transcodingRate = cloudlet.streamPlayRate;
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;

            //increase the job count and load of each vm
            getVmsToJobCountMap().put(vm.getId(), getVmsToJobCountMap().get(vm.getId()) + 1);
            getVmsToJobLoadMap().put(vm.getId(), getVmsToJobLoadMap().get(vm.getId()) + cloudlet.getPredictedCloudletLength());

            vmIndex = getBestVmIndex();
            getCloudletSubmittedList().add(cloudlet);
        }

        // remove submitted cloudlets from waiting list
        for (Cloudlet cloudlet : getCloudletSubmittedList()) {
            getCloudletList().remove(cloudlet);
        }
        schedule(getId(), 5, CLOUDLET_SUBMIT_REQ);
    }

    /**
     * Destroy the virtual machines running in datacenters.
     *
     * @pre $none
     * @post $none
     */
    protected void clearDatacenters() {
        for (Vm vm : getVmsCreatedList()) {
            Log.printConcatLine(CloudSim.clock(), ": " + getName(), ": Destroying VM #", vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.VM_DESTROY, vm);
        }

        getVmsCreatedList().clear();
    }

    /**
     * Send an internal event communicating the end of the simulation.
     *
     * @pre $none
     * @post $none
     */
    protected void finishExecution() {
        sendNow(getId(), CloudSimTags.END_OF_SIMULATION);
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#shutdownEntity()
     */
    @Override
    public void shutdownEntity() {
        if(this.bw != null)
            try {
                this.bw.close();
        } catch (IOException ex) {
            Logger.getLogger(TranscodingDatacenterBroker.class.getName()).log(Level.SEVERE, null, ex);
        }
        Log.printConcatLine(getName(), " is shutting down...");
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#startEntity()
     */
    @Override
    public void startEntity() {
        Log.printConcatLine(getName(), " is starting...");
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
        schedule(getId(), 2, VM_CREATE_REQ);
        schedule(getId(), 2, VM_DESTROY_REQ);
        schedule(getId(), 5, CLOUDLET_SUBMIT_REQ);
    }

    /**
     * Gets the vm list.
     *
     * @param <T> the generic type
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends Vm> List<T> getVmList() {
        return (List<T>) vmList;
    }

    /**
     * Sets the vm list.
     *
     * @param <T> the generic type
     * @param vmList the new vm list
     */
    protected <T extends Vm> void setVmList(List<T> vmList) {
        this.vmList = vmList;
    }

    /**
     * Gets the cloudlet list.
     *
     * @param <T> the generic type
     * @return the cloudlet list
     */
    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getCloudletList() {
        return (List<T>) cloudletList;
    }

    /**
     * Sets the cloudlet list.
     *
     * @param <T> the generic type
     * @param cloudletList the new cloudlet list
     */
    protected <T extends Cloudlet> void setCloudletList(List<T> cloudletList) {
        this.cloudletList = cloudletList;
    }

    /**
     * Gets the cloudlet submitted list.
     *
     * @param <T> the generic type
     * @return the cloudlet submitted list
     */
    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getCloudletSubmittedList() {
        return (List<T>) cloudletSubmittedList;
    }

    /**
     * Sets the cloudlet submitted list.
     *
     * @param <T> the generic type
     * @param cloudletSubmittedList the new cloudlet submitted list
     */
    protected <T extends Cloudlet> void setCloudletSubmittedList(List<T> cloudletSubmittedList) {
        this.cloudletSubmittedList = cloudletSubmittedList;
    }

    /**
     * Gets the cloudlet received list.
     *
     * @param <T> the generic type
     * @return the cloudlet received list
     */
    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getCloudletReceivedList() {
        return (List<T>) cloudletReceivedList;
    }

    /**
     * Sets the cloudlet received list.
     *
     * @param <T> the generic type
     * @param cloudletReceivedList the new cloudlet received list
     */
    protected <T extends Cloudlet> void setCloudletReceivedList(List<T> cloudletReceivedList) {
        this.cloudletReceivedList = cloudletReceivedList;
    }

    /**
     * Gets the vm list.
     *
     * @param <T> the generic type
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends Vm> List<T> getVmsCreatedList() {
        return (List<T>) vmsCreatedList;
    }

    /**
     * Sets the vm list.
     *
     * @param <T> the generic type
     * @param vmsCreatedList the vms created list
     */
    protected <T extends Vm> void setVmsCreatedList(List<T> vmsCreatedList) {
        this.vmsCreatedList = vmsCreatedList;
    }

    /**
     * Gets the vms requested.
     *
     * @return the vms requested
     */
    protected int getVmsRequested() {
        return vmsRequested;
    }

    /**
     * Sets the vms requested.
     *
     * @param vmsRequested the new vms requested
     */
    protected void setVmsRequested(int vmsRequested) {
        this.vmsRequested = vmsRequested;
    }

    /**
     * Gets the vms acks.
     *
     * @return the vms acks
     */
    protected int getVmsAcks() {
        return vmsAcks;
    }

    /**
     * Sets the vms acks.
     *
     * @param vmsAcks the new vms acks
     */
    protected void setVmsAcks(int vmsAcks) {
        this.vmsAcks = vmsAcks;
    }

    /**
     * Increment vms acks.
     */
    protected void incrementVmsAcks() {
        vmsAcks++;
    }

    /**
     * Gets the vms destroyed.
     *
     * @return the vms destroyed
     */
    protected int getVmsDestroyed() {
        return vmsDestroyed;
    }

    /**
     * Sets the vms destroyed.
     *
     * @param vmsDestroyed the new vms destroyed
     */
    protected void setVmsDestroyed(int vmsDestroyed) {
        this.vmsDestroyed = vmsDestroyed;
    }

    /**
     * Gets the datacenter ids list.
     *
     * @return the datacenter ids list
     */
    protected List<Integer> getDatacenterIdsList() {
        return datacenterIdsList;
    }

    /**
     * Sets the datacenter ids list.
     *
     * @param datacenterIdsList the new datacenter ids list
     */
    protected void setDatacenterIdsList(List<Integer> datacenterIdsList) {
        this.datacenterIdsList = datacenterIdsList;
    }

    /**
     * Gets the vms to datacenters map.
     *
     * @return the vms to datacenters map
     */
    protected Map<Integer, Integer> getVmsToDatacentersMap() {
        return vmsToDatacentersMap;
    }

    /**
     * Sets the vms to datacenters map.
     *
     * @param vmsToDatacentersMap the vms to datacenters map
     */
    protected void setVmsToDatacentersMap(Map<Integer, Integer> vmsToDatacentersMap) {
        this.vmsToDatacentersMap = vmsToDatacentersMap;
    }

    /**
     * Gets the datacenter characteristics list.
     *
     * @return the datacenter characteristics list
     */
    protected Map<Integer, DatacenterCharacteristics> getDatacenterCharacteristicsList() {
        return datacenterCharacteristicsList;
    }

    /**
     * Sets the datacenter characteristics list.
     *
     * @param datacenterCharacteristicsList the datacenter characteristics list
     */
    protected void setDatacenterCharacteristicsList(
            Map<Integer, DatacenterCharacteristics> datacenterCharacteristicsList) {
        this.datacenterCharacteristicsList = datacenterCharacteristicsList;
    }

    /**
     * Gets the datacenter requested ids list.
     *
     * @return the datacenter requested ids list
     */
    protected List<Integer> getDatacenterRequestedIdsList() {
        return datacenterRequestedIdsList;
    }

    /**
     * Sets the datacenter requested ids list.
     *
     * @param datacenterRequestedIdsList the new datacenter requested ids list
     */
    protected void setDatacenterRequestedIdsList(List<Integer> datacenterRequestedIdsList) {
        this.datacenterRequestedIdsList = datacenterRequestedIdsList;
    }

    public int calculateProvision(BufferedWriter bw) throws IOException {

        int vmsToAdd = 0;
        List<Vm> vmList = getVmsCreatedList();
        
        if(getVmsCreatedList().size() == 0)
            return BASE_NO_VMS;

        long totalLoad = this.getVmsToJobLoadMap().get(getVmsCreatedList().get(0).getId());
        for (int i = 1; i < getVmsCreatedList().size(); i++) {
            totalLoad = totalLoad + getVmsToJobLoadMap().get(getVmsCreatedList().get(i).getId());
        }

        double avWaitingTime = (double)((totalLoad / MIPS ) / getVmsCreatedList().size());
        vmsToAdd = (int)((totalLoad/MIPS) / SLA_WAITING_TIME) -  getVmsCreatedList().size();

        Log.printLine(CloudSim.clock() + "Provisioned : " + vmsToAdd + " Virtual Machines ");
        String writeString = totalLoad / MIPS + "\t" + getVmsCreatedList().size() + "\t" + avWaitingTime;
        bw.write(writeString);
        bw.newLine();
        bw.flush();
        return vmsToAdd;
    }

    protected int getBestVmIndex() {

        int vmSelected = 0;
        if (this.lbAlgo.equals("qlen")) {
            double vmJobCount;
            vmJobCount = getVmsToJobCountMap().get(getVmsCreatedList().get(0).getId());
            for (int i = 1; i < getVmsCreatedList().size(); i++) {
                if (vmJobCount > getVmsToJobCountMap().get(getVmsCreatedList().get(i).getId())) {
                    TranscodingVm vm = (TranscodingVm) getVmsCreatedList().get(i);
                    if (vm.getCloseToComplete() == false) {
                        vmJobCount = getVmsToJobCountMap().get(getVmsCreatedList().get(i).getId());
                        vmSelected = i;
                    }
                }
            }
        } else {
            long vmJobLoad;
            vmJobLoad = getVmsToJobLoadMap().get(getVmsCreatedList().get(0).getId());
            for (int i = 1; i < getVmsCreatedList().size(); i++) {
                //Log.printLine("prev load = " + vmJobLoad + " curr load" +getVmsToJobLoadMap().get(getVmsCreatedList().get(i).getId()) );
                if (vmJobLoad > getVmsToJobLoadMap().get(getVmsCreatedList().get(i).getId())) {
                    //Log.printLine("*****I am here*****");
                    TranscodingVm vm = (TranscodingVm) getVmsCreatedList().get(i);
                    if (vm.getCloseToComplete() == false) {
                        vmJobLoad = getVmsToJobLoadMap().get(getVmsCreatedList().get(i).getId());
                        vmSelected = i;
                    }
                }
            }
        }
        return vmSelected;

    }

    /**
     * Sets the vms to datacenters map.
     *
     * @param vmsToDatacentersMap the vms to datacenters map
     */
    protected void setVmsToJobCountMap(Map<Integer, Integer> vmsToJobCountMap) {
        this.VmsToJobCountMap = vmsToJobCountMap;
    }

    protected Map<Integer, Integer> getVmsToJobCountMap() {
        return VmsToJobCountMap;
    }

    protected void setVmsToJobLoadMap(Map<Integer, Long> vmsToJobLoadMap) {
        this.VmsToJobLoadMap = vmsToJobLoadMap;
    }

    protected Map<Integer, Long> getVmsToJobLoadMap() {
        return VmsToJobLoadMap;
    }

    public void setLbAlgo(String lbAlgo) {
        this.lbAlgo = lbAlgo;
    }

    public String getLbAlgo() {
        return lbAlgo;
    }

    public void setResultWriter(BufferedWriter bw) {
        this.bw = bw;
    }

    public BufferedWriter getResultWriter() {
        return this.bw;
    }
    
    public void setEndOfSimulation(boolean endOfSimulation) {
        this.endOfSimulation = endOfSimulation;
    }

    public boolean getEndOfSimulation() {
        return this.endOfSimulation;
    }
}
