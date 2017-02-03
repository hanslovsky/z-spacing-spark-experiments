package de.hanslovsky.zspacing.spark.experiments.mask;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.SerializationHelper;

public class WekaClassifierMaskGenerator< U extends RealType< U > > implements MaskGenerator< Instance, U >
{

	private final Classifier classifier;

	private final int classIndex;

	public WekaClassifierMaskGenerator( final String classifierPath, final int classIndex ) throws Exception
	{
		super();
		this.classifier = ( Classifier ) SerializationHelper.read( classifierPath );
		this.classIndex = classIndex;
	}

	@Override
	public void generateMask( final RandomAccessible< Instance > image, final RandomAccessibleInterval< U > mask ) throws Exception
	{

		for ( final Pair< Instance, U > p : Views.interval( Views.pair( image, mask ), mask ) )
		{
			final Instance i = p.getA();
			final double[] pred = classifier.distributionForInstance( i );
			p.getB().setReal( pred[ classIndex ] );
		}
	}

}
