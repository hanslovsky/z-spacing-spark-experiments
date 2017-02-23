package de.hanslovsky.zspacing.spark.experiments;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.janelia.thickness.ScaleOptions;
import org.janelia.thickness.kryo.KryoSerialization;
import org.janelia.thickness.matrix.ComputeMatrices;
import org.janelia.utility.MatrixStripConversion;

import ij.ImageJ;
import ij.ImagePlus;
import loci.formats.FormatException;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
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
//		final String experiment = "20170202_120337";
		final String experiment = "20170201_202843";
		final String configPath = path + "/" + experiment + "/config.json";
		final ScaleOptions scaleOptions = ScaleOptions.createFromFile( configPath );

//		ZSpacing.run( sc, scaleOptions, true );
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

		final long r = halfStrip.dimension( 0 ) - 1;
		final Img< T > img = fac.create( new long[] { r * 2 + 1, halfStrip.dimension( 1 ) }, t );

		for ( final Pair< T, T > p : Views.interval( Views.pair( Views.offset( img, r, 0 ), halfStrip ), halfStrip ) )
			p.getA().set( p.getB() );

		final ExtendedRandomAccessibleInterval< T, RandomAccessibleInterval< T > > ext = Views.extendValue( halfStrip, t );

		for ( long i = 1; i <= r; ++i )
		{
			final Cursor< T > target = Views.hyperSlice( img, 0, r - i ).cursor();
			final Cursor< T > source = Views.offsetInterval( Views.hyperSlice( ext, 0, i ), new long[] { -i }, new long[] { halfStrip.dimension( 1 ) } ).cursor();
			while ( source.hasNext() )
				target.next().set( source.next() );
		}

		return img;
	}

}
