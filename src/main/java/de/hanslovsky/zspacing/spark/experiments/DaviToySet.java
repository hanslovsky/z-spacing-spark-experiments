package de.hanslovsky.zspacing.spark.experiments;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.janelia.thickness.ComputeMatrices;
import org.janelia.thickness.ScaleOptions;
import org.janelia.thickness.ZSpacing;
import org.janelia.thickness.kryo.KryoSerialization;
import org.janelia.utility.MatrixStripConversion;

import ij.ImageJ;
import ij.ImagePlus;
import loci.formats.FormatException;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBounds;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import pl.joegreen.lambdaFromString.LambdaCreationException;

public class DaviToySet
{

	public static void main( final String[] args ) throws FormatException, IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, LambdaCreationException, InterruptedException, ExecutionException
	{

		final SparkConf conf = new SparkConf().setMaster( "local[*]" ).setAppName( MethodHandles.lookup().lookupClass().getSimpleName() ).set( "spark.serializer", "org.apache.spark.serializer.KryoSerializer" ).set( "spark.kryo.registrator", KryoSerialization.Registrator.class.getName() );
		final JavaSparkContext sc = new JavaSparkContext( conf );

		LogManager.getRootLogger().setLevel( Level.ERROR );
		//		 ComputeMatrices.SubSectionCorrelations.LOG.setLevel( Level.DEBUG );

		final String path = "/data/hanslovskyp/davi_toy_set/z-position-spark-test";
		final String experiment = "20170202_120337";
		final String configPath = path + "/" + experiment + "/config.json";
		final ScaleOptions scaleOptions = ScaleOptions.createFromFile( configPath );

		ZSpacing.run( sc, scaleOptions );
		ComputeMatrices.run( sc, scaleOptions );

		new ImageJ();
		final ImagePlus imp = new ImagePlus( path + "/" + experiment + "/out/00/matrices/(0,0).tif" );
		final Img< FloatType > strip = ImageJFunctions.wrapFloat( imp );
		imp.show();
		final RandomAccessibleInterval< FloatType > full = halfStripToStrip( strip, new ArrayImgFactory<>(), new FloatType( Float.NaN ) );
		ImageJFunctions.show( full, "full" );
		ImageJFunctions.show( MatrixStripConversion.stripToMatrix( full, new FloatType( Float.NaN ) ), "matrix" );

		sc.close();

	}

	public static < T extends NumericType< T > > Img< T > halfStripToStrip( final RandomAccessibleInterval< T > halfStrip, final ImgFactory< T > fac, final T t )
	{
		final T t1 = t.createVariable();
		final T t0 = t.createVariable();
		t1.setOne();
		t0.setZero();

		final long r = halfStrip.dimension( 0 );
		final Img< T > img = fac.create( new long[] { r * 2 + 1, halfStrip.dimension( 1 ) }, t );
		for ( final T v : Views.hyperSlice( img, 0, r ) )
			v.setOne();

		for ( final Pair< T, T > p : Views.interval( Views.pair( Views.offset( img, r + 1, 0 ), halfStrip ), halfStrip ) )
			p.getA().set( p.getB() );


		final OutOfBounds< T > access = Views.extendValue( halfStrip, t ).randomAccess();

		for ( final Cursor< T > c = Views.interval( img, new FinalInterval( new long[] { r, img.dimension( 1 ) } ) ).cursor(); c.hasNext(); )
		{
			final T v = c.next();
			final long x = c.getLongPosition( 0 );
			final long y = c.getLongPosition( 1 );
			final long d = r - x + 1;
			access.setPosition( new long[] { d, y - d } );
			System.out.println( new Point( c ) + " " + new Point( access ) );
			v.set( access.get() );
		}

		return img;
	}

}
