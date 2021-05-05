//package org.alindner.cish.lang;
//
//import com.github.dockerjava.api.DockerClient;
//import com.github.dockerjava.api.command.BuildImageResultCallback;
//import com.github.dockerjava.api.model.BuildResponseItem;
//import com.github.dockerjava.core.DefaultDockerClientConfig;
//import com.github.dockerjava.core.DockerClientImpl;
//import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
//
//import java.util.Set;
//
///**
// * adds docker interaction
// * <p>
// * todo finish
// */
//public class Docker {
//	static DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("unix:///var/run/docker.sock").build();
//	static DockerClient              docker = DockerClientImpl.getInstance(
//			Docker.config,
//			new ZerodepDockerHttpClient.Builder()
//					.dockerHost(Docker.config.getDockerHost())
//					.sslConfig(Docker.config.getSSLConfig())
//					.maxConnections(100)
//					.build()
//	);
//
//	/**
//	 * build a docker image
//	 *
//	 * @param file dockerfile
//	 * @param name imagename
//	 */
//	public static void build(final String file, final String name) {
//		final BuildImageResultCallback callback = new BuildImageResultCallback() {
//			@Override
//			public void onNext(final BuildResponseItem item) {
//				System.out.print(item.getStream());
//				super.onNext(item);
//			}
//		};
//
//
//		final String id = Docker.docker.buildImageCmd(new CiFile(file))
//
//		                               //  .withPull(true)
//		                               //    .withNoCache(true)
//		                               .withTags(Set.of(name))
//		                               .exec(callback)
//		                               .awaitImageId();
//	}
//}