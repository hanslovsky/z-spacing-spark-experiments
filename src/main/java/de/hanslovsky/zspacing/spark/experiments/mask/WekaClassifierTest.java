package de.hanslovsky.zspacing.spark.experiments.mask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import de.hanslovsky.zspacing.spark.experiments.mask.weka.InstanceView;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.DoubleArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import net.imglib2.view.composite.RealComposite;

public class WekaClassifierTest
{

	public static void main( final String[] args ) throws Exception
	{

		final String classifierPath = "/data/hanslovskyp/weka-test/grayvalue.model";
		final String imgPath = "/data/hanslovskyp/weka-test/img.tif";

		final RandomAccessibleInterval< RealComposite< FloatType > > img = Views.collapseReal( Views.addDimension( ImageJFunctions.wrapFloat( new ImagePlus( imgPath ) ), 0, 0 ) );
		final InstanceView< FloatType > instances = new InstanceView<>( img, InstanceView.makeDefaultAttributes( 1, 2 ) );
		System.out.println( "Created InstanceView: " + Arrays.toString( Intervals.dimensionsAsLongArray( img ) ) );

		final ArrayImg< DoubleType, DoubleArray > mask = ArrayImgs.doubles( Intervals.dimensionsAsLongArray( img ) );

		final BdvStackSource< FloatType > bdv = BdvFunctions.show( Converters.convert( img, ( s, t ) -> {
			t.set( s.get( 0 ).get() * 255 );
		}, new FloatType() ), "img", BdvOptions.options().is2D() );
		System.out.println( "Showing img. " );

		final WekaClassifierMaskGenerator< DoubleType > generator = new WekaClassifierMaskGenerator<>( classifierPath, 0 );

		final int nThreads = Runtime.getRuntime().availableProcessors() - 1;
		final long stepSize = img.dimension( 1 ) / nThreads;
		final ArrayList< Callable< Void > > tasks = new ArrayList<>();


		for ( int y = 0; y < img.dimension( 1 ); y += stepSize )
		{
			final IntervalView< DoubleType > interval = Views.interval( mask, new long[] { 0, y }, new long[] { img.max( 0 ), Math.min( y + stepSize - 1, img.max( 1 ) ) } );
			tasks.add( () -> {
				generator.generateMask( instances, interval );
				return null;
			} );
		}

		final ExecutorService es = Executors.newFixedThreadPool( nThreads );
		final List< Future< Void > > fs = es.invokeAll( tasks );
		for ( final Future< Void > f : fs )
			f.get();

		System.out.println( "Generated mask." );


		BdvFunctions.show( Converters.convert( ( RandomAccessibleInterval< DoubleType > ) mask, ( s, t ) -> {
			t.set( 125 << ( s.get() > 0.5 ? 16 : 8 ) );
		}, new ARGBType() ), "mask", BdvOptions.options().addTo( bdv ) );



	}

}
