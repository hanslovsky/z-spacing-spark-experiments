package de.hanslovsky.zspacing.spark.experiments.mask;

import java.io.File;
import java.lang.invoke.MethodHandles;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.janelia.thickness.utility.Utility;

import ij.process.FloatProcessor;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.real.FloatType;

public class GenerateMasks
{

	public static Logger LOG = LogManager.getLogger( MethodHandles.lookup().lookupClass() );
	static
	{
		LOG.setLevel( Level.INFO );
	}

	public static void run(
			final JavaSparkContext sc,
			final String sourcePattern,
			final String targetPattern,
			final int start,
			final int stop,
			final int step,
			final Broadcast< MaskGenerator< FloatType, FloatType > > generator )
	{

		new File( String.format( targetPattern, start ) ).getParentFile().mkdirs();

		final JavaPairRDD< Integer, FloatProcessor > masks = sc.parallelize( Utility.arange( start, stop, step ) ).mapToPair( new Utility.LoadFileFromPattern( sourcePattern ) ).mapValues( fp -> {
			final int w = fp.getWidth();
			final int h = fp.getHeight();
			final float[] arr = new float[ w * h ];
			generator.getValue().generateMask( ArrayImgs.floats( ( float[] ) fp.getPixels(), w, h ), ArrayImgs.floats( arr, w, h ) );
			return new FloatProcessor( w, h, arr );
		} );

		final long count = masks.mapToPair( new Utility.WriteToFormatString<>( targetPattern ) ).count();
		LOG.info( "Wrote " + count + " mask images." );
	}

}
