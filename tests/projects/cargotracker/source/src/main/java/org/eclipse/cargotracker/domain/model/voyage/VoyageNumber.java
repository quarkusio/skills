package org.eclipse.cargotracker.domain.model.voyage;

import java.io.Serializable;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotEmpty;

@Embeddable
public class VoyageNumber implements Serializable {

  private static final long serialVersionUID = 1L;

  @Column(name = "voyage_number")
  @NotEmpty(message = "Voyage number cannot be empty.")
  @JsonProperty("number")
  private String number;

  public VoyageNumber() {
    // Nothing to initialize.
  }

  public VoyageNumber(String number) {
    Objects.requireNonNull(number);

    this.number = number;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null) {
      return false;
    }

    if (!(o instanceof VoyageNumber)) {
      return false;
    }

    VoyageNumber other = (VoyageNumber) o;

    return sameValueAs(other);
  }

  @Override
  public int hashCode() {
    return number.hashCode();
  }

  boolean sameValueAs(VoyageNumber other) {
    return other != null && this.number.equals(other.number);
  }

  @Override
  public String toString() {
    return number;
  }

  @JsonGetter("number")
  public String getIdString() {
    return number;
  }
}
