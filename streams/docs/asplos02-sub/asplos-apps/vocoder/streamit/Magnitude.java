import streamit.*;
import streamit.io.*;

class MagnitudeStuff extends Pipeline {
  public void init(final int DFTLen, final int newLen, final float speed) {
//      final int interpolate = (int) ((speed * 10) + 0.5f; 
    //with this uncommented and used down in the split join, it was
    //complaining about a non-constant field access, even though
    //there's no reason why it should be a field.
    add(new SplitJoin() {
	public void init() {
	  setSplitter(DUPLICATE());
	  add(new FIRSmoothingFilter(DFTLen));
	  add(new IdentityFloat());
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Deconvolve());
    add(new SplitJoin() {
	public void init() {
	  setSplitter(ROUND_ROBIN());
	  add(new Duplicator(DFTLen, newLen));
	  add(new Remapper(DFTLen, newLen));
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Multiplier());
    add(new SplitJoin() {
	public void init() {
	  setSplitter(ROUND_ROBIN());
	  for(int i=0; i < DFTLen; i++) {
	    add(new Remapper(10,  (int) ((speed * 10) + 0.5f)));
	  }
	  setJoiner(ROUND_ROBIN());
	}
      });
  }

  MagnitudeStuff(final int DFTLen, final int newLen, final float speed) {
    super(DFTLen, newLen, speed);
  }
}
