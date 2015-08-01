/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

/**
 * An example showing how to create simulation entities (a DatacenterBroker in
 * this example) in run-time using a globar manager entity (GlobalBroker).
 */
public class TranscodingCloudSim {

    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {
        Log.printLine("Starting TranscodingCloudSim ...");

        try {

            // Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 2;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // First step: Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);
            
            // Second step: Create Datacenters
            // Datacenters are the resource providers in CloudSim. 
            // We need at list one of them to run a CloudSim simulation
            @SuppressWarnings("unused")
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            // Third step: create broker and workload generator entities
            TranscodingDatacenterBroker broker = createBroker("TranscodingBroker_0");
            TranscodingWorkLoadGenerator genrator = new TranscodingWorkLoadGenerator("TranscodingWorkLoadGenerator_0",broker);

            // Forth step: Starts the simulation. job creation, and vm managment will be 
            // taken care of by golbal and datasenter brokers  
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            // List<Cloudlet> newList = broker.getCloudletReceivedList();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            //newList.addAll(globalBroker.getBroker().getCloudletReceivedList());

            CloudSim.stopSimulation();

            printCloudletList(newList);

            Log.printLine("TransodingCloudSim finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static Datacenter createDatacenter(String name) {

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store one or more
        //    Machines
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
        //    create a list to store these PEs before creating
        //    a Machine.
        List<Pe> peList1 = new ArrayList<Pe>();

        long mips = 124162 * 4 * 1000; //enough mips 

        // 3. Create PEs and add these into the list.
        //for a quad-core machine, a list of 4 PEs is required:
        peList1.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
        peList1.add(new Pe(1, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(2, new PeProvisionerSimple(mips)));
        peList1.add(new Pe(3, new PeProvisionerSimple(mips)));

        //Another list, for a dual-core machine
        //List<Pe> peList2 = new ArrayList<Pe>();
        //peList2.add(new Pe(0, new PeProvisionerSimple(mips)));
        //peList2.add(new Pe(1, new PeProvisionerSimple(mips)));

        //4. Create Hosts with its id and list of PEs and add them to the list of machines
        int hostId = 0;
        int ram = 4096 * 1000; //host memory (MB) * no. hosts
        long storage = 4 * 100000 * 1000; //host storage
        int bw = 4 * 10000* 1000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList1,
                        new VmSchedulerTimeShared(peList1)
                )
        ); // This is our first machine
        
        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.1;	// the cost of using storage in this resource
        double costPerBw = 0.1;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    //own broker policy to manage vms and cloudlets according
    //to our specific rules of simulated scenario
    private static TranscodingDatacenterBroker createBroker(String name) {

        TranscodingDatacenterBroker broker = null;
        try {
            broker = new TranscodingDatacenterBroker(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Prints the Cloudlet objects
     *
     * @param list list of Cloudlets
     */
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent + indent + dft.format(cloudlet.getActualCPUTime())
                        + indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }

    }

    public static class TranscodingWorkLoadGenerator extends SimEntity {

        private final static int CREATE_JOBS = 1;
        private final static long MIPS = 124162;
        private static BufferedReader arrivalsReader;
        private static BufferedReader videosReader;
        private static BufferedWriter resultsWriter;
        private final static String arrivalsFileName = "/home/tdeneke/Papers/Proactive_Management/experiment/bambuser_5sec_arrival_count_first_day.csv";
        private final static String videosFileName = "/home/tdeneke/Papers/Proactive_Management/experiment/nn_tr_utime_prediction.csv";
        private final static String resultsFileName = "/home/tdeneke/Papers/Proactive_Management/experiment/nn_provisioning_result.csv";

        private final TranscodingDatacenterBroker broker;
        private List<Cloudlet> cloudletList;
        private int cloudletId = 0;

        public TranscodingWorkLoadGenerator(String name, TranscodingDatacenterBroker broker) {
            super(name);
            this.broker = broker;
        }

        @Override
        public void processEvent(SimEvent ev) {
            switch (ev.getTag()) {
                case CREATE_JOBS:
                    processNewJobs();
                    break;
                    
                default:
                    Log.printLine(getName() + ": unknown event type");
                    break;
            }
        }

        public void processNewJobs() {
            String readLine = null;
            String[] nnparams;
            int arrivals = 0;
            //default cloudlet parameters. will be read from input experimental file
            long length = 40000;
            long predictedLength = (long) (length * 0.95);
            long fileSize = 300;
            long outputSize = 300;
            int pesNumber = 1;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            try {

                if (arrivalsReader.ready() && (readLine = arrivalsReader.readLine()) != null) {
                    arrivals = Integer.parseInt(readLine);
                    arrivals = (int)(arrivals / 5); //faster simulation for bambuser
                } else {
                    broker.setEndOfSimulation(true);
                    if (videosReader != null) {
                        videosReader.close();
                    }
                    if (arrivalsReader != null) {
                        arrivalsReader.close();
                    }
                    return;
                }

                // Creates a container to store Cloudlets
                LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
                TranscodingCloudlet[] cloudlet = new TranscodingCloudlet[arrivals];
                //Cloudlet[] cloudlet = new Cloudlet[arrivals];

                for (int i = 0; i < arrivals; i++) {
                    if (videosReader.ready() && (readLine = videosReader.readLine()) != null) {
                        nnparams = readLine.split(";", -1);
                        predictedLength = (long) Math.abs(MIPS * Double.parseDouble(nnparams[21]));
                        length = (long) Math.abs(MIPS * Double.parseDouble(nnparams[20]));
                        cloudlet[i] = new TranscodingCloudlet(cloudletId++, length, predictedLength, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                        //setting the owner of these Cloudlets
                        cloudlet[i].setUserId(broker.getId());
                        list.add(cloudlet[i]);
                    } else {
                        if (videosReader != null) {
                            videosReader.close();
                        }
                        videosReader = new BufferedReader(new FileReader(videosFileName));
                        readLine = videosReader.readLine(); //skip header
                        i--;
                    }
                }

                setCloudletList(list);
                broker.submitCloudletList(getCloudletList());
                schedule(getId(), 5, CREATE_JOBS);

            } catch (IOException e) {
                System.out.println("file reading problem");
                System.out.println(e);
            }

        }

        @Override
        public void startEntity() {
            try {
                Log.printLine(super.getName() + " is starting...");
                // input output file readers and writers
                resultsWriter = new BufferedWriter(new FileWriter(resultsFileName));
                arrivalsReader = new BufferedReader(new FileReader(arrivalsFileName));
                videosReader = new BufferedReader(new FileReader(videosFileName));
                String header = videosReader.readLine(); //skip header
                header = arrivalsReader.readLine(); //skip header
                broker.setResultWriter(resultsWriter); //pass the result file writer to broker
                System.out.println(header);
                schedule(getId(), 5, CREATE_JOBS); //start generaating job after 5 sec.
            } catch (IOException ex) {
                Logger.getLogger(TranscodingCloudSim.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void shutdownEntity() {
        }

        public List<Cloudlet> getCloudletList() {
            return cloudletList;
        }

        protected void setCloudletList(List<Cloudlet> cloudletList) {
            this.cloudletList = cloudletList;
        }

    }

}
