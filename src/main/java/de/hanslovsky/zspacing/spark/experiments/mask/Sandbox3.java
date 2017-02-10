package de.hanslovsky.zspacing.spark.experiments.mask;

import org.apache.spark.api.java.function.Function;
import org.janelia.thickness.kryo.KryoSerialization;
import org.janelia.thickness.kryo.ij.FloatProcessorSerializer;
import org.janelia.thickness.utility.Utility;
import org.janelia.thickness.utility.Utility.LoadPNGFromPattern;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeOutput;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import net.imglib2.util.RealSum;
import pl.joegreen.lambdaFromString.LambdaFactory;
import pl.joegreen.lambdaFromString.TypeReference;

public class Sandbox3
{

	public static void main( final String[] args ) throws Exception
	{

		final Function< Integer, FloatProcessor > f = LambdaFactory.get().createLambda( "i -> new ij.ImagePlus(String.format( \"/data/hanslovskyp/davi_toy_set/data/seq/%04d.tif\", i ) ).getProcessor().convertToFloatProcessor()", new TypeReference< Function< Integer, FloatProcessor > >()
		{} );

		final FloatProcessor fp = f.call( 4 );
		System.out.println( fp.getf( 0, 3 ) );

		// /data/hanslovskyp/Test_slab_1_98401_fine_nbrs_20/data/094073.0.png
		final LoadPNGFromPattern pngLoader = new Utility.LoadPNGFromPattern( "/data/hanslovskyp/Test_slab_1_98401_fine_nbrs_20/data/%06d.0.png" );
		final int i = 94073;

		new ImageJ();
		new ImagePlus( "test", pngLoader.call( i ) ).show();

		final FloatProcessor fp2 = pngLoader.call( i ).convertToFloatProcessor();
		final float[] data = ( float[] ) fp2.getPixels();
		System.out.println( data.length );
		final KryoSerialization ks = new KryoSerialization();
		final FloatProcessorSerializer fser = new FloatProcessorSerializer();

		final Output op = new Output( 1, 8 * data.length );

		fser.write( new Kryo(), op, fp2 );

		System.out.println( op.toBytes().length );

		final Output op2 = new Output( 1, 8 * data.length );
		new Kryo().writeClassAndObject( op2, data );

		System.out.println( op2.toBytes().length );

		final UnsafeOutput op3 = new UnsafeOutput( 1, 8 * data.length );
		op3.writeFloats( data );

		System.out.println( op3.toBytes().length );
		op3.close();

		final RealSum rs = new RealSum();
		for ( int k = 0; k < 1600; ++k )
			rs.add( 1.0 );
		rs.getSum();

	}

}
