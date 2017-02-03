package de.hanslovsky.zspacing.spark.experiments.mask;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;

public interface MaskGenerator< T, U >
{

	public void generateMask( RandomAccessible< T > image, RandomAccessibleInterval< U > mask ) throws Exception;

}
