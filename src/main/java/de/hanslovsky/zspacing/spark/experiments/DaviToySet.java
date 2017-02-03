package de.hanslovsky.zspacing.spark.experiments;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.janelia.thickness.KryoSerialization;
import org.janelia.thickness.ScaleOptions;
import org.janelia.thickness.ZSpacing;

import loci.formats.FormatException;

public class DaviToySet
{

	public static void main( final String[] args ) throws FormatException, IOException
	{

		final SparkConf conf = new SparkConf()
				.setMaster( "local[*]" )
				.setAppName( MethodHandles.lookup().lookupClass().getSimpleName() )
				.set( "spark.serializer", "org.apache.spark.serializer.KryoSerializer" )
				.set( "spark.kryo.registrator", KryoSerialization.Registrator.class.getName() );
		final JavaSparkContext sc = new JavaSparkContext( conf );

		LogManager.getRootLogger().setLevel( Level.ERROR );

		final String path = "/data/hanslovskyp/davi_toy_set/z-position-spark-test";
		final String experiment = "20170202_120337";
		final String configPath = path + "/" + experiment + "/config.json";
		final ScaleOptions scaleOptions = ScaleOptions.createFromFile( configPath );

		ZSpacing.run( sc, scaleOptions );

		sc.close();

	}

}
