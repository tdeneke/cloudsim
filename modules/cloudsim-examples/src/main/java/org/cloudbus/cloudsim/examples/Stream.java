package org.cloudbus.cloudsim.examples;

import java.util.Map;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;


public class Stream {
	//private double startTime;
	private int id;
	private int transcodingId;//which transcoding is to be performed on it
	private long filesize; //total number of frames
	private long no_i_frames;
        private long frames;
        private long duration;
	private long jobamount;
	private long transcodingtime;
	private double inputfps;
	private double outputfps;
	private double playrate; 
        private double transcodingfps;
        private Map<String, Long> predictedLength;

	public Stream(int id,int transcodingId, long transcodingtime, long i_frames, double infps,double outfps,long filesize, long duration, long frames) {
		
		// TODO Auto-generated constructor stub
		this.id = id;
		this.transcodingId = transcodingId;
		this.transcodingtime = transcodingtime;
		this.no_i_frames = i_frames;
		this.inputfps = infps;
		this.outputfps = outfps;
		this.filesize= filesize;
                this.duration = duration;
                this.frames = frames;
                this.transcodingfps = this.frames * (this.outputfps / this.inputfps);
	}
        
         public long getFrames() {
            //return file size;
            return this.frames;
	}
        
        public long getFileSize() {
            //return file size;
            return this.filesize;
	}
        
        public void setFileSize(long filesize) {
            //set file size;
            this.filesize = filesize;
	}
        
        public double getTranscodingFps() {
            //return output fps;
            return this.transcodingfps;
	}
        
        public double getOutputFps() {
            //return output fps;
            return this.outputfps;
	}
        
        public double getInputFps() {
            //return input fps;
            return this.inputfps;
	}
        
        public long getTranscodingTime() {
            //return startTime;
            return this.transcodingtime;
	}
        
        public int getID() {
            //return id;
            return this.id;
	}
        
        
        public boolean setPredictedLength(final Map predictedLength) {
            this.predictedLength = predictedLength;
            return true;
	}
        
        public Map<String, Long> getPredictedLength(){
                return predictedLength;
        }
	

}
