package de.hanslovsky.zspacing.spark.experiments.mask;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import org.janelia.thickness.similarity.CorrelationsImgLib;
import org.janelia.thickness.utility.Utility;

import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import net.imglib2.RandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class Sandbox2
{

	public static void main( final String[] args ) throws Exception
	{

		for ( int i = 10; i < 20; ++i )
			System.out.println( new Random().nextInt( 30 ) );

		System.out.println( Arrays.toString( IntStream.range( 1, 2 ).toArray() ) );

		final ByteProcessor img1 = new ByteProcessor( 100, 200 );
		final ByteProcessor img2 = new ByteProcessor( 100, 200 );
		final Random rng = new Random( 100 );
		for ( int i = 0; i < img1.getWidth() * img1.getHeight(); ++i )
		{
			img1.set( i, rng.nextInt( 256 ) );
			img2.set( i, rng.nextInt( 256 ) );
		}
		final ByteProcessor bp = Utility.constValueByteProcessor( 100, 200, (byte )1);
		new ImageJ();
		new ImagePlus( "", bp ).show();
		new ImagePlus( "", img1 ).show();
		new ImagePlus( "", img2 ).show();
		final Img< ? extends RealType< ? > > i1 = ImageJFunctions.wrapReal( new ImagePlus( "", img1 ) );
		final Img< ? extends RealType< ? > > i2 = ImageJFunctions.wrapReal( new ImagePlus( "", img2 ) );
		final Img< ? extends RealType< ? > > m = ImageJFunctions.wrapReal( new ImagePlus( "", bp ) );
		final RandomAccessible< ? extends Pair< ? extends RealType< ? >, ? extends RealType< ? > > > p1 = Views.pair( i1, m );
		final RandomAccessible< ? extends Pair< ? extends RealType< ? >, ? extends RealType< ? > > > p2 = Views.pair( i2, m );
		System.out.println( CorrelationsImgLib.calculate( p1, p2, i1 ) + " " + CorrelationsImgLib.calculate( p1, p1, i1 ) );

	}

}
