/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.contract.verifier.builder;

class MethodMetadataBuilder {

	private final SingleMethodBuilder singleMethodBuilder;

	private final BlockBuilder builder;

	private final GeneratedClassMetaData metaData;

	MethodMetadataBuilder(SingleMethodBuilder singleMethodBuilder) {
		this.singleMethodBuilder = singleMethodBuilder;
		this.builder = singleMethodBuilder.blockBuilder;
		this.metaData = singleMethodBuilder.generatedClassMetaData;
	}

	MethodMetadataBuilder java() {
		this.singleMethodBuilder
				.methodMetadata(new JavaMethodMetadata(this.builder, this.metaData));
		return this;
	}

	MethodMetadataBuilder groovy() {
		this.singleMethodBuilder
				.methodMetadata(new GroovyMethodMetadata(this.builder, this.metaData));
		return this;
	}

	MethodMetadataBuilder kotlin() {
		this.singleMethodBuilder
				.methodMetadata(new KotlinMethodMetadata(this.builder, this.metaData));
		return this;
	}

	SingleMethodBuilder build() {
		return this.singleMethodBuilder;
	}

}
