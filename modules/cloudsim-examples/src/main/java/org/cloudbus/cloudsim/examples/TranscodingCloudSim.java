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
    private final static long MIPS = 124162;
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
            TranscodingWorkLoadGenerator generator = new TranscodingWorkLoadGenerator("TranscodingWorkLoadGenerator_0",broker);

            // Forth step: Starts the simulation. job creation, and vm managment will be 
            // taken care of by golbal and datasenter brokers  
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            // List<Cloudlet> newList = broker.getCloudletReceivedList();
            List<Cloudlet> newList = broker.getCloudletReceivedList();
            //newList.addAll(globalBroker.getBroker().getCloudletReceivedList());

            CloudSim.stopSimulation();

            printCloudletList(newList, generator.getCloudletResultsWriter());

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

        long mips = 124162 * 4 * 200; //enough mips 

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
    private static void printCloudletList(List<Cloudlet> list, BufferedWriter writer) throws IOException {
        int size = list.size();
        TranscodingCloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
                + "Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Submitted Time" + indent + "Start Time" + indent + "Finish Time");
        writer.write("Cloudlet ID" + "\t" + "STATUS" + "\t"
                + "Data center ID" + "\t" + "VM ID" + "\t" + "Time" + "\t" + "Submitted Time" + "\t" + "Start Time" + "\t" + "Finish Time" + "\t" + "Waiting Time" + "\t" + "Est. Waiting Time");
        writer.newLine();
        
        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = (TranscodingCloudlet)list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);
            writer.write(cloudlet.getCloudletId() + "\t");

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                writer.write("1");

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId()
                        + indent + indent + indent + dft.format(cloudlet.getActualCPUTime())
                        + indent + indent + indent + dft.format(cloudlet.getSubmissionTime())
                        + indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getFinishTime()));
                writer.write("\t" + cloudlet.getResourceId() + "\t" + cloudlet.getVmId()
                        + "\t" + dft.format(cloudlet.getActualCPUTime())
                        + "\t" + dft.format(cloudlet.getSubmissionTime())
                        + "\t" + dft.format(cloudlet.getExecStartTime()) 
                        + "\t" + dft.format(cloudlet.getFinishTime())
                        + "\t" + dft.format(cloudlet.getExecStartTime() - cloudlet.getSubmissionTime())
                        + "\t" + dft.format(cloudlet.getPredictedWaitingLength()/MIPS)+ "\n");
            }
        }

    }

    public static class TranscodingWorkLoadGenerator extends SimEntity {

        private final static int CREATE_JOBS = 1;
        //private final static long MIPS = 124162;
        private static BufferedReader arrivalsReader;
        private static BufferedReader videosReader;
        private static BufferedWriter resultsWriter;
        private static BufferedWriter cloudletResultsWriter;
        private final static String arrivalsFileName = "/home/tdeneke/Papers/Proactive_Management/experiment/bambuser_5sec_arrival_count_first_week.csv";
        private final static String videosFileName = "/home/tdeneke/Papers/Proactive_Management/experiment/nn_segment_prediction.csv";
        private final static String resultsFileName = "/home/tdeneke/Papers/Proactive_Management/experiment/ideal_provisioning_result.csv";
        private final static String cloudletResultsFileName = "/home/tdeneke/Papers/Proactive_Management/experiment/ideal_provisioning_cloudlet_result.csv";

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
            long frames = 300;
            long fileSize = 300;
            long outputSize = 300;
            int pesNumber = 1;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            try {

                if (arrivalsReader.ready() && (readLine = arrivalsReader.readLine()) != null) {
                    arrivals = Integer.parseInt(readLine);
                    //we assume transcoding request is 120* less likely than streaming request
                    //can be changed as needed.
                    arrivals = (int)(arrivals / 20); 
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
                        
                        //predictedLength = (long) Math.abs(MIPS * Double.parseDouble(nnparams[21]));
                        //length = (long) Math.abs(MIPS * Double.parseDouble(nnparams[20]));
                        
                        predictedLength = (long) Math.abs(MIPS * Double.parseDouble(nnparams[18]));
                        length = (long) Math.abs(MIPS * Double.parseDouble(nnparams[19]));
                        //output video frames = (output video fps / input video fps) * input vodeo frames
                        //frames = (long) Math.abs(Double.parseDouble(nnparams[16])*Double.parseDouble(nnparams[8])*Double.parseDouble(nnparams[4]));
                        frames = (long) Math.abs(Double.parseDouble(nnparams[13])*Double.parseDouble(nnparams[7])*Double.parseDouble(nnparams[3]));
                        cloudlet[i] = new TranscodingCloudlet(cloudletId++, length, predictedLength, frames, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
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
                cloudletResultsWriter = new BufferedWriter(new FileWriter(cloudletResultsFileName));
                arrivalsReader = new BufferedReader(new FileReader(arrivalsFileName));
                videosReader = new BufferedReader(new FileReader(videosFileName)); 
                String [] features = videosReader.readLine().split(";", -1);//skip header
                //print important feature headers to check
                //System.out.println(features[4] +"\t"+ features[8] +"\t"+ features[16] +"\t"+ features[20] +"\t"+ features[21]);
                System.out.println(features[3] +"\t"+ features[7] +"\t"+ features[13] +"\t"+ features[18] +"\t"+ features[19]);

                String header = arrivalsReader.readLine(); //skip header
                broker.setResultWriter(resultsWriter); //pass the result file writer to broker
                header = "vms" + "\t" + "predictedAVTime" + "\t" + "actualAVWaitingTime" + "\t" + "slaWaitingTime" + "\t" + "totalJobs" + "\t" + "totalPredictedTime" + "\t" + "totalPredictedFps" + "\t" + "totalActualTime" + "\t" + "totalActualFps" + "\t" + "avUtilization";
                broker.getResultWriter().write(header);
                broker.getResultWriter().newLine();
        
                System.out.println(header);
                schedule(getId(), 185, CREATE_JOBS); //start generaating job after 3 min.can be changed
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

        public void setCloudletList(List<Cloudlet> cloudletList) {
            this.cloudletList = cloudletList;
        }
        
        public BufferedWriter getCloudletResultsWriter() {
            return cloudletResultsWriter;
        }


    }

}
