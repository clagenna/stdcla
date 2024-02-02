package sm.clagenna.stdcla.javafx;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

public class ImageViewResizer extends Region {

  private ObjectProperty<ImageView> imageViewProperty = new SimpleObjectProperty<ImageView>();

  public ObjectProperty<ImageView> imageViewProperty() {
    return imageViewProperty;
  }

  public ImageView getImageView() {
    return imageViewProperty.get();
  }

  public void setImageView(ImageView imageView) {
    this.imageViewProperty.set(imageView);
  }

  public ImageViewResizer() {
    this(new ImageView());
  }

  @Override
  protected void layoutChildren() {
    ImageView imageView = imageViewProperty.get();
    if (imageView != null) {
      imageView.setPreserveRatio(true);
      // System.out.printf("ImageViewResizer(%d, %d) %%%.2f \n", (int) getWidth(), (int) getHeight(), getWidth() / getHeight());
      imageView.setFitWidth(getWidth());
      // imageView.setFitHeight(getHeight());
      layoutInArea(imageView, 0, 0, getWidth(), getHeight(), 0, HPos.CENTER, VPos.CENTER);
    }
    super.layoutChildren();
  }

  public ImageViewResizer(ImageView imageView) {
    imageViewProperty.addListener(new ChangeListener<ImageView>() {

      @Override
      public void changed(ObservableValue<? extends ImageView> arg0, ImageView oldIV, ImageView newIV) {
        if (oldIV != null) {
          getChildren().remove(oldIV);
        }
        if (newIV != null) {
          getChildren().add(newIV);
        }
      }
    });
    this.imageViewProperty.set(imageView);
  }
}
