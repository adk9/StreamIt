/*
 * First cut of Darpa PCA Demo in StreamIt.
 */

import streamit.*;


public class PCADemo extends StreamIt
{
  static public void main(String[] t)
  {
    PCADemo test = new PCADemo();
    test.run();
  }

  public void init()
  {

    System.out.println("Running the PCA Demo...");
    final int numChannels           = 48;
    final int numSamples            = 4096;
    final int numBeams              = 16;
    final int numCoarseFilterTaps   = 16;
    final int numFineFilterTaps     = 64;
    final int coarseDecimationRatio = 1;
    final int fineDecimationRatio   = 1;
    final int numSegments           = 1;
    final int numPostDec1           = numSamples/coarseDecimationRatio;
    final int numPostDec2           = numPostDec1/fineDecimationRatio;
    final int mfSize                = numSegments*numPostDec2;
    final int pulseSize             = numPostDec2/2;
    final int predecPulseSize       = pulseSize*coarseDecimationRatio*fineDecimationRatio;
    final int targetBeam            = numBeams/4;
    final int targetSample          = numSamples/4;
    final int targetSamplePostdec   = 1 + targetSample/coarseDecimationRatio/fineDecimationRatio;
    final float dOverLambda         = 0.5f;
    final float cfarThreshold       = 0.95f*dOverLambda*numChannels*(0.5f*pulseSize);

    add(new DataSource(numChannels, numSamples, numBeams,
		       targetBeam, targetSample, predecPulseSize));
    add(new FirFilter(numCoarseFilterTaps));
    add(new FirFilter(numFineFilterTaps));
    add(new BeamFormer(numBeams, numChannels, numSamples));
    add(new FirFilter(mfSize));
    add(new ComplexToMag ());
    add(new TestDetector(numBeams, numSamples, targetBeam,
			 targetSample, cfarThreshold));
  }
}
