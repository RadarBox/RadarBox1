package org.rdr.radarbox.DSP;

import android.graphics.Canvas;

import org.rdr.radarbox.R;
import org.rdr.radarbox.RadarBox;

public class SNR {
    int nAccumulated = 10;
    int nFrequencies;
    int nSnrAccumulated = 10;
    short[][] accumulatedData;
    float[] arrayMaxSNR;
    float[] arrayAvgSNR;
    float maxSNR = 0;
    float avgSNR = 0;
    float[] arraySNR;
    int iFrame = 0;
    int iSNR = 0;
    private int FN, rxN, txN, chN, frameSize, freqInitMHz, freqStepMHz;

    public SNR(){
        // Get another class variables
        this.FN = RadarBox.freqSignals.getFN();
        this.chN = RadarBox.freqSignals.getChN();
        this.frameSize = RadarBox.freqSignals.getFrameSize();
        this.rxN = RadarBox.freqSignals.getRxN();
        this.txN = RadarBox.freqSignals.getTxN();
        this.freqInitMHz = RadarBox.freqSignals.getFreqInitMHz();
        this.freqStepMHz = RadarBox.freqSignals.getFreqStepMHz();
        // Init
        arrayMaxSNR = new float[nSnrAccumulated];
        arrayAvgSNR = new float[nSnrAccumulated];
        this.nFrequencies = 2*FN*chN;
        accumulatedData = new short[nAccumulated][nFrequencies];
        arraySNR = new float[nFrequencies];
    }

    public void calculateSNR(short[] rawFreqFrame){
        // Raw Data Accumulation
        for (int f=0; f<nFrequencies; f++)
            accumulatedData[iFrame][f] = rawFreqFrame[f];
        iFrame++;
        if (iFrame >= nAccumulated)
            iFrame = 0;

        // Null SNRs
        for (int i=0; i<nSnrAccumulated; i++) {
            arrayAvgSNR[i] =0;
            arrayMaxSNR[i] = 0;
        }
        // Variance Calculation
        float mu ;
        for(int f=0; f<nFrequencies; f++){
            // Mu
            mu = 0;
            for (int n=0; n<nAccumulated; n++)
                mu += accumulatedData[n][f];
            mu /= nAccumulated;
            // Variance
            for (int n=0; n<nAccumulated; n++)
                arraySNR[f] = (float)Math.pow((accumulatedData[n][f]-mu),2);
            arraySNR[f] /= nAccumulated-1;
            // Maximum SNR
            if (arraySNR[f]>arrayMaxSNR[iSNR])
                arrayMaxSNR[iSNR] = arraySNR[f];
            // Average SNR
            arrayAvgSNR[iSNR] += arraySNR[f];
        }
        arrayAvgSNR[iSNR] /= nFrequencies;

        iSNR++;
        if (iSNR >= nSnrAccumulated) {
            // Average estimation
            avgSNR = 0;
            maxSNR = 0;
            for (int i=0; i<nSnrAccumulated; i++){
                avgSNR += arrayAvgSNR[i];
                maxSNR += arrayMaxSNR[i];
            }
            avgSNR /= nSnrAccumulated;
            maxSNR /= nSnrAccumulated;
            iSNR = 0;
        }
    }

    public float getAvgSNR() {
        return avgSNR;
    }
    public float getMaxSNR() {
        return maxSNR;
    }
}
