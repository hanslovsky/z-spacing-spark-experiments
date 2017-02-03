package de.hanslovsky.zspacing.spark.experiments.mask;

import java.lang.invoke.MethodHandles;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.janelia.thickness.KryoSerialization;

public class GenerateMasksLocal
{

	public static void main( final String[] args )
	{
		final SparkConf conf = new SparkConf().setMaster( "local[*]" ).setAppName( MethodHandles.lookup().lookupClass().getSimpleName() ).set( "spark.serializer", "org.apache.spark.serializer.KryoSerializer" ).set( "spark.kryo.registrator", KryoSerialization.Registrator.class.getName() );
		final JavaSparkContext sc = new JavaSparkContext( conf );
		LogManager.getRootLogger().setLevel( Level.ERROR );

		final int start = 1;
		final int stop = 7;
		final int step = 8;

		final String sourcePattern = "/data/hanslovskyp/hk-test/data/%06d.0.png";
		final String targetPattern = "/data/hanslovskyp/hk-test/mask/%06d.0.png";

		//		GenerateMasks.run( sc, sourcePattern, targetPattern, start, stop, step, sc.broadcast( new IgnoreFilterMaskGenerator<>( f -> f.get() < 140.0 ) ) );
		GenerateMasks.run( sc, sourcePattern, targetPattern, start, stop, step, sc.broadcast( new VarianceThresholdMaskGenerator<>( 100.0, 100, 100 ) ) );

		sc.close();

	}

}
