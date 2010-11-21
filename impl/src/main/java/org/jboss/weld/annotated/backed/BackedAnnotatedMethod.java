package org.jboss.weld.annotated.backed;

import static java.util.Collections.unmodifiableList;
import static org.jboss.weld.util.reflection.Reflections.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;

import com.google.common.collect.ImmutableSet;

public class BackedAnnotatedMethod<X> extends BackedAnnotatedMember<X> implements AnnotatedMethod<X>
{
 
   public static <X, Y extends X> AnnotatedMethod<X> of(AnnotatedMethod<X> originalMethod, AnnotatedType<Y> declaringType)
   {
      AnnotatedType<X> downcastDeclaringType = cast(declaringType);
      return new BackedAnnotatedMethod<X>(originalMethod.getBaseType(), downcastDeclaringType, originalMethod.getParameters(), originalMethod.getJavaMember());
   }
   
   private final Method method;
   private final List<AnnotatedParameter<X>> parameters;

   public BackedAnnotatedMethod(Type baseType, AnnotatedType<X> declaringType, List<AnnotatedParameter<X>> originalParameters, Method method)
   {
      super(baseType, declaringType);
      this.method = method;
      List<AnnotatedParameter<X>> parameters = new ArrayList<AnnotatedParameter<X>>(originalParameters.size());
      for (AnnotatedParameter<X> originalParameter : originalParameters)
      {
         parameters.add(new BackedAnnotatedParameter<X>(originalParameter.getBaseType(), originalParameter.getAnnotations(), originalParameter.getPosition(), this));
      }
      this.parameters = unmodifiableList(parameters);
   }

   public Method getJavaMember()
   {
      return method;
   }
   
   public <T extends Annotation> T getAnnotation(Class<T> annotationType)
   {
      return method.getAnnotation(annotationType);
   }
   
   public Set<Annotation> getAnnotations()
   {
      return ImmutableSet.copyOf(method.getAnnotations());
   }
   
   public boolean isAnnotationPresent(Class<? extends Annotation> annotationType)
   {
      return method.isAnnotationPresent(annotationType);
   }
   
   public List<AnnotatedParameter<X>> getParameters()
   {
      return parameters;
   }

}
