Pod::Spec.new do |spec|
  spec.name = 'EncryptionSharedKit'
  spec.version = '1.0.0'
  spec.homepage = 'https://www.cocoapods.org'
  spec.source = { :git => "git@github.com:aalluhaibi/encryption-kit-kmp.git" }
  spec.authors = 'Abdulrahman Alluhaibi'
  spec.summary = 'Encryption Shared Kit'
  spec.static_framework = true
  spec.vendored_frameworks = "EncryptionSharedKit.xcframework"
  spec.ios.deployment_target = '15'
end