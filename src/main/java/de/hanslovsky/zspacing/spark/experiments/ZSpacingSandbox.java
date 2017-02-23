package de.hanslovsky.zspacing.spark.experiments;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.janelia.thickness.SparkInference;
import org.janelia.thickness.inference.InferFromMatrix;
import org.janelia.thickness.inference.InferFromMatrix.RegularizationType;
import org.janelia.thickness.inference.Options;
import org.janelia.thickness.inference.fits.GlobalCorrelationFitAverage;
import org.janelia.thickness.inference.visitor.LazyVisitor;
import org.janelia.thickness.lut.LUTRealTransform;
import org.janelia.utility.MatrixStripConversion;

import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.InverseRealTransform;
import net.imglib2.realtransform.RealTransformRandomAccessible;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class ZSpacingSandbox
{

	public static void main( final String[] args ) throws Exception
	{

		final String matrixPath = "resources/matrices/(0,0).tif";
		final String weightPath = "resources/weight-matrices/(0,0).tif";

		final Img< FloatType > matrix = ImageJFunctions.wrapFloat( new ImagePlus( matrixPath ) );
		final Img< FloatType > weight = ImageJFunctions.wrapFloat( new ImagePlus( weightPath ) );
		final double[] coord = IntStream.range( 0, ( int ) matrix.dimension( 1 ) ).mapToDouble( k -> k ).toArray();
		final double[] scale = IntStream.range( 0, ( int ) matrix.dimension( 1 ) ).mapToDouble( i -> 1 ).toArray();
		final double[] shift = IntStream.range( 0, ( int ) matrix.dimension( 1 ) ).mapToDouble( i -> 1 ).toArray();

		new ImageJ();

		final InferFromMatrix inf = new InferFromMatrix( new GlobalCorrelationFitAverage() );
		final Options opt = Options.generateDefaultOptions();
		opt.scalingFactorRegularizerWeight = 0.1;
		opt.coordinateUpdateRegularizerWeight = 0.0;
		opt.shiftProportion = 0.6;
		opt.nIterations = 100;
		opt.comparisonRange = ( int ) matrix.dimension( 0 );
		opt.minimumSectionThickness = 1e-8;
		opt.regularizationType = RegularizationType.BORDER;
		opt.scalingFactorEstimationIterations = 10;
		opt.withReorder = false;
		opt.nThreads = 1;
		opt.forceMonotonicity = true;
		opt.estimateWindowRadius = -1;
		opt.minimumCorrelationValue = 0.0;
		final FloatType t = new FloatType( Float.NaN );
		final ArrayImgFactory< FloatType > fac = new ArrayImgFactory<>();
		final RandomAccessibleInterval< FloatType > m = MatrixStripConversion.stripToMatrix( SparkInference.halfStripToStrip( matrix, fac, t ), t );
		final RandomAccessibleInterval< FloatType > w = MatrixStripConversion.stripToMatrix( SparkInference.halfStripToStrip( weight, fac, t ), t );
		ImageJFunctions.show( m, "matrix" );
		ImageJFunctions.show( w, "weight" );
		final double[] lut = inf.estimateZCoordinates( m, coord, new double[ 0 ], scale, w, shift, new LazyVisitor(), opt );
//		final double[] lut = inf.estimateZCoordinates( m, coord, new double[ 0 ], scale, ConstantUtils.constantRandomAccessibleInterval( new FloatType( 1.0f ), 2, m ), shift, new LazyVisitor(), opt );
		System.out.println( Arrays.toString( lut ) );
		final LUTRealTransform tf = new LUTRealTransform( lut, 2, 2 );
		final RealTransformRandomAccessible< FloatType, InverseRealTransform > tfed = RealViews.transform( Views.interpolate( Views.extendValue( m, new FloatType( Float.NaN ) ), new NLinearInterpolatorFactory<>() ), tf );
		ImageJFunctions.show( Views.interval( Views.raster( tfed ), m ), "tfed" );


	}

}
